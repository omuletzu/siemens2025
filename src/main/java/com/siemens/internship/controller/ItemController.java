package com.siemens.internship.controller;

import com.siemens.internship.service.ItemService;
import com.siemens.internship.model.Item;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    // logger folosit pentru a afisa eventuale erori sau informatii suplimentare
    private final Logger LOGGER = LoggerFactory.getLogger(ItemController.class);

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    // aceasta metoda e folosita in createItem si updateItem asadar am facut o functie separata pentru a nu duplica codul
    private ResponseEntity<?> getResponseEntity(BindingResult result) {
        if(result.hasErrors()) {

            // aici errMsg va contine toate erorile din urma validarii
            StringBuilder errMsg = new StringBuilder();

            // se trece prin fiecare eroare
            result.getAllErrors().forEach(err -> {
                // si adauga in mesajul final de eroare
                errMsg.append(err.getDefaultMessage()).append("; ");
            });

            LOGGER.error("Error while creating item, error : " + errMsg.toString());

            // aici se returneaza erorile si se foloseste BAD_REQUEST (inainte era CREATED)
            return new ResponseEntity<>(errMsg.toString(), HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {

        // se verifica daca a aparut vreo eroare in urma validarii
        ResponseEntity<?> errMsg = getResponseEntity(result);
        if (errMsg != null) {
            return errMsg;
        }

        Item createdItem = itemService.save(item);

        // daca totul a descurs bine se returneaza item-ul cu succes
        LOGGER.info("Succesfully create item ID" + createdItem.getId());
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))

                // ar trebui folosit NOT_FOUND in loc de NO_CONTENT pentru ca request-ul nu a fost procesat cu succes
                .orElseGet(() -> {
                    LOGGER.warn("Item with ID " + id + " not found");
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @PutMapping("/{id}")    // ar trebui folosit si aici @Valid pentru a ne asigura cu item-ul pe care vrem sa-l schimbam respecta regulile
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {

        // se verifica daca in urma validarii a aparut vreo eroare
        ResponseEntity<?> errMsg = getResponseEntity(result);
        if (errMsg != null) {
            return errMsg;
        }

        // aici se cauta item-ul in baza de date
        Optional<Item> existingItem = itemService.findById(id);

        // daca exista ar trebui modificat continutul acestuia
        if (existingItem.isPresent()) {

            // aici se modifica id-ul item-ului ce urmeaza sa fie salvat in locul celui vechi pentru consistenta
            item.setId(id);

            // aici se salveaza item-ul
            Item modifiedItem = itemService.save(item);

            LOGGER.info("Updated item with ID " + id);

            // daca a fost cu succes se returneaza item-ul actualizat si ar trebui folosit OK in loc de CREATED
            return new ResponseEntity<>(modifiedItem, HttpStatus.OK);
        } else {
            LOGGER.warn("Item not found with ID " + id);

            // daca nu exista ar trebui returnat NOT_FOUND, nu ACCEPTED
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {

        // inainte sa stergem item-ul cu acel id verificam daca exista in baza de date
        Optional<Item> item = itemService.findById(id);

        // daca exista atunci se sterge
        if(item.isPresent()) {
            itemService.deleteById(id);

            LOGGER.info("Deleted item with ID " + id);

            // aici ar trebui returnat NO_CONTENT, nu CONFLICT, deoarece nu exista vreun conflict cu item-ul sters
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        else {
            LOGGER.warn("Item with ID " + id + " doesn't exist, so nothing was deleted");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        try {
            // aici se foloseste o metoda asincrona pentru a procesa toate itemele
            CompletableFuture<List<Item>> futureItemList = itemService.processItemsAsync();

            // aici se va astepta terminarea procesarii tuturor item-elor (ele sunt procesate in paralel)
            List<Item> itemList = futureItemList.join();

            // aici se returneaza lista elementelor procesate, fiind returnate cu succes
            return new ResponseEntity<>(itemList, HttpStatus.OK);
        }
        catch (Exception e) {
            LOGGER.error("Unexpected error while processing items", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
