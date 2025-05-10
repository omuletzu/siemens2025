package com.siemens.internship;

import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
public class InternshipApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Test
    void createItem_test_success() throws Exception {
        Item item = new Item();
        item.setId(1L);
        item.setName("item");
        item.setDescription("description");
        item.setEmail("valid@mail.com");

        when(itemService.save(any(Item.class))).thenReturn(item);

        String jsonBody = """
            {
                "name": "item",
                "description": "description",
                "email": "valid@mail.com"
            }
            """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated());
    }

    @Test
    void createItem_test_failure() throws Exception {
        String jsonBody = """
            {
                "name": "item",
                "description": "description",
                "email": "invalid-mail"
            }
            """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Wrong email format")));
    }

    @Test
    void getItemById_test_success() throws Exception {
        Item item = new Item(1L, "item", "description", "NEPROCESAT", "valid@mail.com");

        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getItemById_test_failure() throws Exception {
        when(itemService.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItem_test_success() throws Exception {
        Item modifiedItem = new Item(1L, "modifiedItem", "description", "NEPROCESAT", "valid@mail.com");

        when(itemService.findById(1L)).thenReturn(Optional.of(new Item()));
        when(itemService.save(any(Item.class))).thenReturn(modifiedItem);

        String jsonBody = """
            {
                "name": "modifiedItem",
                "description": "description",
                "email": "valid@mail.com"
            }
            """;

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("modifiedItem")));
    }

    @Test
    void updateItem_test_failure() throws Exception {
        when(itemService.findById(10L)).thenReturn(Optional.empty());

        String jsonBody = """
            {
                "name": "non-existing",
                "description": "blah blah",
                "email": "valid@mail.com"
            }
            """;

        mockMvc.perform(put("/api/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_test_success() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(new Item()));

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItem_test_failure() throws Exception {
        when(itemService.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/items/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void processItems_test_success() throws Exception {
        List<Item> itemList = List.of(new Item(1L, "item", "description", "NEPROCESAT", "valid@mail.com"));

        when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(itemList));

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk());
    }
}
