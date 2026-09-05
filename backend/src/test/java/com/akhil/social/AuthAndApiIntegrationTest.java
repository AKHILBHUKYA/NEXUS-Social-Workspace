package com.akhil.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerAndLogin(String user, String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"" + user + "\"}"))
                .andExpect(status().isOk());
        MvcResult r = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + user + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void healthUp() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("nexus-backend"));
    }

    @Test
    void registerLoginMe() throws Exception {
        String token = registerAndLogin("testuser1", "t1@nexus.test");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser1"));
    }

    @Test
    void invalidLogin() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/posts")).andExpect(status().isForbidden());
    }

    @Test
    void postLikeUnlikeCommentShareSave() throws Exception {
        String token = registerAndLogin("poster1", "p1@nexus.test");
        MvcResult create = mvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"instagram\",\"content\":\"Hello NEXUS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello NEXUS"))
                .andReturn();
        long postId = mapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        // duplicate like
        mvc.perform(post("/api/posts/" + postId + "/like").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/posts/" + postId + "/like").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0));

        mvc.perform(post("/api/comments/post/" + postId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Nice post\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/comments/post/" + postId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Nice post"));

        mvc.perform(post("/api/posts/" + postId + "/share").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareCount").value(1));

        mvc.perform(post("/api/posts/" + postId + "/save").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saveCount").value(1));

        mvc.perform(delete("/api/posts/" + postId + "/save").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saveCount").value(0));

        mvc.perform(delete("/api/posts/" + postId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cannotDeleteOthersPost() throws Exception {
        String t1 = registerAndLogin("owner1", "o1@nexus.test");
        String t2 = registerAndLogin("other1", "o2@nexus.test");
        MvcResult create = mvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + t1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"x\",\"content\":\"Mine\"}"))
                .andExpect(status().isOk()).andReturn();
        long id = mapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/posts/" + id).header("Authorization", "Bearer " + t2))
                .andExpect(status().isForbidden());
    }

    @Test
    void messagesAndContacts() throws Exception {
        String token = registerAndLogin("msguser", "m@nexus.test");
        mvc.perform(post("/api/messages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"platform\":\"whatsapp\",\"conversation\":\"Rahul\",\"content\":\"Hi there\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hi there"));

        mvc.perform(get("/api/messages?platform=whatsapp&conversation=Rahul")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hi there"));

        mvc.perform(post("/api/contacts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Rahul Varma\",\"initials\":\"RV\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/contacts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rahul Varma"));
    }

    @Test
    void analyticsAndSearchAndNotifications() throws Exception {
        String token = registerAndLogin("anuser", "a@nexus.test");
        mvc.perform(get("/api/analytics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists());

        mvc.perform(get("/api/search?q=an").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray());

        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void aiFallbackWithoutKey() throws Exception {
        String token = registerAndLogin("aiuser", "ai@nexus.test");
        MvcResult r = mvc.perform(post("/api/ai/chat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"summarize workspace\",\"context\":\"{}\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = mapper.readTree(r.getResponse().getContentAsString());
        assertTrue(body.get("success").asBoolean());
        assertNotNull(body.get("reply").asText());
    }

    @Test
    void followUnfollow() throws Exception {
        String t1 = registerAndLogin("follow1", "f1@nexus.test");
        String t2 = registerAndLogin("follow2", "f2@nexus.test");
        MvcResult me = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + t2))
                .andExpect(status().isOk()).andReturn();
        long id2 = mapper.readTree(me.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/follows/" + id2).header("Authorization", "Bearer " + t1))
                .andExpect(status().isOk());
        mvc.perform(post("/api/follows/" + id2).header("Authorization", "Bearer " + t1))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/follows/" + id2).header("Authorization", "Bearer " + t1))
                .andExpect(status().isOk());
    }
}
