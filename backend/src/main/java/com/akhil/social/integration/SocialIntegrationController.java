package com.akhil.social.integration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.akhil.social.entity.User;
import java.net.URI;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
@RestController @RequestMapping("/api/social")
public class SocialIntegrationController {
 private final SocialIntegrationService service;
 @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl;
 public SocialIntegrationController(SocialIntegrationService s){service=s;}
 @GetMapping("/providers") public Object providers(){return service.providers();}
 @GetMapping("/connections") public Object connections(@AuthenticationPrincipal User u){return service.status(u.getId());}
 @GetMapping("/connect/{platform}") public Object connect(@PathVariable String platform,@AuthenticationPrincipal User u){return Map.of("url",service.authorize(platform,u.getId()),"platform",platform);}
 @GetMapping("/callback/{platform}") public ResponseEntity<Void> callback(@PathVariable String platform,@RequestParam String code,@RequestParam String state){service.callback(platform,code,state);return ResponseEntity.status(302).location(URI.create(frontendUrl+"/?socialConnected="+platform)).build();}
 @GetMapping("/status") public Object status(@AuthenticationPrincipal User u){return service.connectionDetails(u.getId());}
 @DeleteMapping("/connections/{platform}") public Object disconnect(@PathVariable String platform,@AuthenticationPrincipal User u){service.disconnect(u.getId(),platform);return Map.of("ok",true);}
 @PostMapping("/x/post") public Object xPost(@AuthenticationPrincipal User u,@RequestBody Map<String,String> b){return service.xPost(u.getId(),b.getOrDefault("text",""));}
 @PostMapping("/meta/{platform}/post") public Object metaPost(@PathVariable String platform,@AuthenticationPrincipal User u,@RequestBody Map<String,String> b){return service.metaPost(u.getId(),platform,b.getOrDefault("text",""));}
 @PostMapping("/instagram/container") public Object igContainer(@AuthenticationPrincipal User u,@RequestBody Map<String,String> b){return service.instagramContainer(u.getId(),b.get("imageUrl"),b.getOrDefault("caption",""));}
 @PostMapping("/instagram/publish") public Object igPublish(@AuthenticationPrincipal User u,@RequestBody Map<String,String> b){return service.instagramPublish(u.getId(),b.get("creationId"));}
 @PostMapping("/publish") public Object publish(@AuthenticationPrincipal User u,@RequestBody Map<String,String> b){return service.publish(u.getId(),b.getOrDefault("platform",""),b.getOrDefault("text",""),b.get("imageUrl"));}
 @GetMapping("/webhook") public ResponseEntity<String> verifyWebhook(@RequestParam(name="hub.mode",required=false) String mode,@RequestParam(name="hub.verify_token",required=false) String token,@RequestParam(name="hub.challenge",required=false) String challenge){if("subscribe".equals(mode) && token!=null && token.equals(System.getenv().getOrDefault("META_WEBHOOK_VERIFY_TOKEN","nexus-verify"))) return ResponseEntity.ok(challenge==null?"":challenge); return ResponseEntity.status(403).body("Forbidden");}
 @PostMapping(value="/webhook", consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<Void> webhook(@RequestBody String payload){service.handleMetaWebhook(payload);return ResponseEntity.ok().build();}
 @PostMapping("/whatsapp/send") public Object wa(@AuthenticationPrincipal User u,@RequestBody Map<String,String> b){return service.whatsappSend(u.getId(),b.get("to"),b.get("text"));}
}
