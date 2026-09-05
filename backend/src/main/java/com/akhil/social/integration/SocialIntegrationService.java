package com.akhil.social.integration;

import com.akhil.social.entity.SocialConnection;
import com.akhil.social.entity.User;
import com.akhil.social.repository.SocialConnectionRepository;
import com.akhil.social.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocialIntegrationService {
 private final SocialConnectionRepository repo; private final UserRepository users; private final ObjectMapper mapper;
 private final RestClient http=RestClient.create();
 @Value("${app.backend-url:http://localhost:8080}") String backendUrl;
 @Value("${social.meta.client-id:}") String metaId; @Value("${social.meta.client-secret:}") String metaSecret;
 @Value("${social.meta.graph-version:v23.0}") String metaVersion;
 @Value("${social.x.client-id:}") String xId; @Value("${social.x.client-secret:}") String xSecret;
 @Value("${social.x.scopes:tweet.read tweet.write users.read offline.access}") String xScopes;
 @Value("${social.whatsapp.phone-number-id:}") String whatsappPhoneNumberId;
 @Value("${social.redirect-path:/api/social/callback}") String callbackPath;
 @Value("${social.webhook.verify-token:nexus-verify}") String webhookVerifyToken;
 private record OAuthState(Long userId, String verifier) {}
 private final Map<String,OAuthState> states=new ConcurrentHashMap<>();
 public SocialIntegrationService(SocialConnectionRepository repo,UserRepository users,ObjectMapper mapper,org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate){this.repo=repo;this.users=users;this.mapper=mapper;this.messagingTemplate=messagingTemplate;}
 private String callback(String p){return backendUrl+callbackPath+"/"+p;}
 public Map<String,Object> providers(){
  Map<String,Object> m=new LinkedHashMap<>();
  m.put("instagram",Map.of("configured",!metaId.isBlank(),"mode","Meta Graph API","features",List.of("profile","media","publishing*","insights*")));
  m.put("facebook",Map.of("configured",!metaId.isBlank(),"mode","Meta Graph API","features",List.of("pages","page feed*","comments*","insights*")));
  m.put("whatsapp",Map.of("configured",!metaId.isBlank(),"mode","WhatsApp Cloud API","features",List.of("send messages*","webhooks*")));
  m.put("x",Map.of("configured",!xId.isBlank(),"mode","X API v2","features",List.of("profile","read posts*","publish posts*")));
  return m;
 }
 public String authorize(String platform,Long userId){
  platform=platform.toLowerCase(Locale.ROOT); String state=UUID.randomUUID().toString(); String verifier=state; states.put(state,new OAuthState(userId,verifier));
  if(platform.equals("instagram")||platform.equals("facebook")||platform.equals("whatsapp")){
   if(metaId.isBlank()) throw new IllegalStateException("Meta app is not configured. Set SOCIAL_META_CLIENT_ID and SOCIAL_META_CLIENT_SECRET.");
   String scopes=platform.equals("whatsapp")?"business_management,whatsapp_business_management,whatsapp_business_messaging": "pages_show_list,pages_read_engagement,pages_manage_posts,instagram_basic,instagram_content_publish,instagram_manage_insights";
   return UriComponentsBuilder.fromUriString("https://www.facebook.com/"+metaVersion+"/dialog/oauth").queryParam("client_id",metaId).queryParam("redirect_uri",callback(platform)).queryParam("state",state).queryParam("scope",scopes).build().toUriString();
  }
  if(platform.equals("x")){
   if(xId.isBlank()) throw new IllegalStateException("X app is not configured. Set SOCIAL_X_CLIENT_ID and SOCIAL_X_CLIENT_SECRET.");
   String scope=URLEncoder.encode(xScopes,StandardCharsets.UTF_8);
   String challenge=pkceChallenge(verifier);
   return "https://twitter.com/i/oauth2/authorize?response_type=code&client_id="+URLEncoder.encode(xId,StandardCharsets.UTF_8)+"&redirect_uri="+URLEncoder.encode(callback("x"),StandardCharsets.UTF_8)+"&scope="+scope+"&state="+state+"&code_challenge="+challenge+"&code_challenge_method=S256";
  }
  throw new IllegalArgumentException("Unsupported platform: "+platform);
 }
 public void callback(String platform,String code,String state){
  OAuthState oauth=states.remove(state); Long uid=oauth==null?null:oauth.userId(); if(oauth==null) throw new IllegalArgumentException("Invalid or expired OAuth state");
  try{
   JsonNode token;
   if(platform.equals("x")) token=exchangeX(code,oauth.verifier()); else token=exchangeMeta(code,platform);
   String access=token.path("access_token").asText(); if(access.isBlank()) throw new IllegalStateException("Provider returned no access token");
   JsonNode profile=platform.equals("x")?getXProfile(access):getMetaProfile(access);
   String connectionToken=access;
   if(platform.equals("facebook")||platform.equals("instagram")){
    JsonNode pages=getMetaPages(access);
    if(pages.path("data").isArray() && pages.path("data").size()>0){
      JsonNode first=pages.path("data").get(0);
      connectionToken=first.path("access_token").asText(access);
      if(platform.equals("facebook")){ profile=first; }
      else if(first.has("instagram_business_account")){ ObjectNode instagramProfile=first.path("instagram_business_account").deepCopy(); instagramProfile.put("name",first.path("name").asText("Instagram")); profile=instagramProfile; }
    }
   }
   if(platform.equals("whatsapp") && !whatsappPhoneNumberId.isBlank()){ ObjectNode whatsappProfile=profile.deepCopy(); whatsappProfile.put("phone_number_id",whatsappPhoneNumberId); profile=whatsappProfile; }
   SocialConnection c=repo.findByUserIdAndPlatform(uid,platform).orElseGet(SocialConnection::new);
   User u=users.findById(uid).orElseThrow(); c.setUser(u); c.setPlatform(platform); c.setAccessToken(connectionToken); c.setRefreshToken(token.path("refresh_token").asText(null)); c.setTokenExpiresAt(token.has("expires_in")?Instant.now().plusSeconds(token.path("expires_in").asLong()):null); c.setExternalId(profile.path("id").asText(null)); c.setDisplayName(profile.path("name").asText(profile.path("username").asText(platform))); c.setMetadataJson(profile.toString()); c.setActive(true); c.setUpdatedAt(Instant.now()); repo.save(c);
  }catch(Exception e){throw new IllegalStateException("OAuth callback failed: "+e.getMessage(),e);}
 }
 private JsonNode exchangeMeta(String code,String platform){return http.post().uri(UriComponentsBuilder.fromUriString("https://graph.facebook.com/"+metaVersion+"/oauth/access_token").queryParam("client_id",metaId).queryParam("client_secret",metaSecret).queryParam("redirect_uri",callback(platform)).queryParam("code",code).build().toUriString()).retrieve().body(JsonNode.class);}
 private JsonNode exchangeX(String code,String verifier){String form="code="+enc(code)+"&grant_type=authorization_code&client_id="+enc(xId)+"&redirect_uri="+enc(callback("x"))+"&code_verifier="+enc(verifier); return http.post().uri("https://api.x.com/2/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);}
 private String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
 private String pkceChallenge(String verifier){try{var md=java.security.MessageDigest.getInstance("SHA-256");return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(verifier.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Unable to create PKCE challenge",e);}}
 private JsonNode getMetaProfile(String token){return http.get().uri("https://graph.facebook.com/"+metaVersion+"/me?fields=id,name&access_token="+enc(token)).retrieve().body(JsonNode.class);}
 private JsonNode getMetaPages(String token){return http.get().uri("https://graph.facebook.com/"+metaVersion+"/me/accounts?fields=id,name,access_token,instagram_business_account&access_token="+enc(token)).retrieve().body(JsonNode.class);}
 private JsonNode getXProfile(String token){return http.get().uri("https://api.x.com/2/users/me?user.fields=id,name,username,profile_image_url,description,public_metrics").header(HttpHeaders.AUTHORIZATION,"Bearer "+token).retrieve().body(JsonNode.class).path("data");}
 public List<Map<String,Object>> status(Long uid){List<Map<String,Object>> out=new ArrayList<>(); for(String p:List.of("whatsapp","instagram","facebook","x")){var c=repo.findByUserIdAndPlatform(uid,p); Map<String,Object> m=new LinkedHashMap<>();m.put("platform",p);m.put("connected",c.isPresent()&&c.get().isActive());m.put("displayName",c.map(SocialConnection::getDisplayName).orElse(null));m.put("externalId",c.map(SocialConnection::getExternalId).orElse(null));m.put("expiresAt",c.map(SocialConnection::getTokenExpiresAt).orElse(null));out.add(m);}return out;}
 public void disconnect(Long uid,String platform){repo.findByUserIdAndPlatform(uid,platform).ifPresent(c->{c.setActive(false);c.setAccessToken(null);c.setRefreshToken(null);repo.save(c);});}
 public JsonNode xPost(Long uid,String text){SocialConnection c=connected(uid,"x"); Map<String,Object> body=Map.of("text",text); return http.post().uri("https://api.x.com/2/tweets").header(HttpHeaders.AUTHORIZATION,"Bearer "+c.getAccessToken()).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);}
 public JsonNode metaPost(Long uid,String platform,String text){SocialConnection c=connected(uid,platform); String base="https://graph.facebook.com/"+metaVersion+"/"; String path=c.getExternalId()+"/feed"; if(platform.equals("instagram")) throw new IllegalArgumentException("Instagram publishing requires a media container and eligible professional account; use /api/social/meta/instagram/container before publish."); return http.post().uri(base+path).contentType(MediaType.APPLICATION_FORM_URLENCODED).body("message="+enc(text)+"&access_token="+enc(c.getAccessToken())).retrieve().body(JsonNode.class);}
 public JsonNode instagramContainer(Long uid,String imageUrl,String caption){SocialConnection c=connected(uid,"instagram"); String url="https://graph.facebook.com/"+metaVersion+"/"+c.getExternalId()+"/media?image_url="+enc(imageUrl)+"&caption="+enc(caption)+"&access_token="+enc(c.getAccessToken());return http.post().uri(url).retrieve().body(JsonNode.class);}
 public JsonNode instagramPublish(Long uid,String creationId){SocialConnection c=connected(uid,"instagram");return http.post().uri("https://graph.facebook.com/"+metaVersion+"/"+c.getExternalId()+"/media_publish?creation_id="+enc(creationId)+"&access_token="+enc(c.getAccessToken())).retrieve().body(JsonNode.class);}
 public JsonNode whatsappSend(Long uid,String to,String text){SocialConnection c=connected(uid,"whatsapp"); try{JsonNode meta=mapper.readTree(c.getMetadataJson());String phoneId=meta.path("phone_number_id").asText(); if(phoneId.isBlank()) throw new IllegalStateException("WhatsApp phone_number_id is missing. Add it to the connection metadata."); String url="https://graph.facebook.com/"+metaVersion+"/"+phoneId+"/messages"; Map<String,Object> body=Map.of("messaging_product","whatsapp","to",to,"type","text","text",Map.of("body",text));return http.post().uri(url).header(HttpHeaders.AUTHORIZATION,"Bearer "+c.getAccessToken()).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}}
 public Map<String,Object> connectionDetails(Long uid){
  List<Map<String,Object>> out=new ArrayList<>();
  for(String p:List.of("whatsapp","instagram","facebook","x")){
   var c=repo.findByUserIdAndPlatform(uid,p);
   Map<String,Object> m=new LinkedHashMap<>(); m.put("platform",p); m.put("connected",c.isPresent()&&c.get().isActive());
   m.put("displayName",c.map(SocialConnection::getDisplayName).orElse(null)); m.put("externalId",c.map(SocialConnection::getExternalId).orElse(null));
   m.put("expiresAt",c.map(SocialConnection::getTokenExpiresAt).orElse(null)); out.add(m);
  } return Map.of("providers",providers(),"connections",out);
 }
 public JsonNode publish(Long uid,String platform,String text,String imageUrl){
  platform=platform.toLowerCase(Locale.ROOT);
  if(text==null||text.isBlank()) throw new IllegalArgumentException("Post text is required");
  if(platform.equals("x")) return xPost(uid,text);
  if(platform.equals("facebook")) return metaPost(uid,"facebook",text);
  if(platform.equals("instagram")){
   if(imageUrl==null||imageUrl.isBlank()) throw new IllegalArgumentException("Instagram requires a public image URL");
   JsonNode c=instagramContainer(uid,imageUrl,text); return instagramPublish(uid,c.path("id").asText());
  }
  throw new IllegalArgumentException("Unsupported publishing platform: "+platform);
 }
 public void handleMetaWebhook(String payload){
  try{
   JsonNode root=mapper.readTree(payload);
   if(!root.path("entry").isArray()) return;
   for(JsonNode entry:root.path("entry")){
    String object=entry.path("id").asText("unknown");
    for(JsonNode change:entry.path("changes")) emitWebhookEvent("meta",object,change);
    for(JsonNode messaging:entry.path("messaging")) emitWebhookEvent("meta",object,messaging);
   }
  }catch(Exception e){throw new IllegalArgumentException("Invalid Meta webhook payload",e);}
 }
 private void emitWebhookEvent(String provider,String externalId,JsonNode event){
  Map<String,Object> msg=Map.of("provider",provider,"externalId",externalId,"receivedAt",Instant.now().toString(),"event",event);
  for(SocialConnection c:repo.findAll()){
   if(c.isActive() && ((c.getExternalId()!=null&&c.getExternalId().equals(externalId)) || (c.getMetadataJson()!=null&&c.getMetadataJson().contains(externalId)))){
    try{messagingTemplate.convertAndSend("/topic/social/"+c.getUser().getId(),msg);}catch(Exception ignored){}
   }
  }
 }
 private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
 private SocialConnection connected(Long uid,String p){return repo.findByUserIdAndPlatform(uid,p).filter(SocialConnection::isActive).orElseThrow(()->new IllegalStateException("Connect "+p+" first in NEXUS Connections."));}
}
