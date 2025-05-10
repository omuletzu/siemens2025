package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    // aceasta lista contine toate elementele finale procesate
    private List<Item> itemsProcessed = new ArrayList<>();

    // counter atomic pentru a numara cate iteme au fost procesate cu succes
    private AtomicInteger processedCount = new AtomicInteger(0);

    // asemanator cu cel de sus doar ca pentru cele fara de succes
    private AtomicInteger failedCount = new AtomicInteger(0);

    // logger folosit pentru a afisa mesaje in timpul executiei
    private final Logger LOGGER = LoggerFactory.getLogger(ItemService.class);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     * <p>
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    /**
     * Aceasta este o metoda asincrona deoarece in ea itemele sunt procesate in paralel
     * Se foloseste cate un thread pentru fiecare item id folosind CompletableFuture
     */
    @Async  // adnotare folosita pentru face metoda sa fie executata asincron
    public CompletableFuture<List<Item>> processItemsAsync() {

        // in itemIds vor fi toate id-urile item-urilor din baza de date
        List<Long> itemIds = itemRepository.findAllIds();

        // aceasta lista contine toate task-urile (cate unul pentru fiecare id), pentru care se va astepta terminarea executiei
        List<CompletableFuture<Void>> futureThreadsList = new ArrayList<>();

        // aceasta variabila este atomica deoarece fiecare thread in parte o poate accesa si modifica
        processedCount = new AtomicInteger(0);

        failedCount = new AtomicInteger(0);

        // aici se foloseste synchronized pentru a face lista thread-safe, deoarece cu aceasta vor lucra mai multe thread-uri
        itemsProcessed = Collections.synchronizedList(new ArrayList<>());

        // se trece prin fiecare id
        for (Long id : itemIds) {

            // acesta reprezinta un task asincron ce proceseaza item-ul (este cate unul pentru fiecare id)
            CompletableFuture<Void> futureThread = CompletableFuture.runAsync(() -> {
               try {

                   // se obtine item-ul din baza de data dupa id
                   Optional<Item> optionalItem = itemRepository.findById(id);

                   //daca este gasit se proceseaza
                   if (optionalItem.isPresent()) {
                       Item item = optionalItem.get();

                       // item-ul se proceseaza si se salveaza inapoi in baza de date
                       item.setStatus("PROCESSED");
                       itemRepository.save(item);

                       // se incrementeaza atomic numarul de elemente procesate si se adauga in lista de iteme procesate
                       itemsProcessed.add(item);
                       processedCount.incrementAndGet();
                   }
                   //daca item-ul nu este gasit
                   else {

                       // se incrementeaza numarul de elemente ce nu au putut fi procesate
                       failedCount.incrementAndGet();

                       // se afiseaza un mesaj corespunzator
                       LOGGER.warn("Item not found with ID " + id);
                   }
               }
               catch (Exception e) {
                    // elementul nu a putut fi procesat deci creste numarul elementelor neprocesate si se afiseaza un mesaj corespunzator
                    failedCount.incrementAndGet();
                    LOGGER.error("Error while processing item ID " + id);
               }
            }, executor);

            // task-ul curent este adaugat in lista cu toate task-urile
            futureThreadsList.add(futureThread);
        }

        return CompletableFuture
                .allOf(futureThreadsList.toArray(new CompletableFuture[0])) // asa se asteapta terminarea tuturor task-urilor din lista
                .thenApply(x -> {   // dupa ce toate sunt gata se afiseaza cate au procesate in total, cate cu succes si cate nu
                    LOGGER.info("Processed: " + itemsProcessed.size() + " items, successfully: " + processedCount.get() + " , failed : " + failedCount.get());

                    // aici se returneaza lista de elemente procesate
                    return itemsProcessed;
                })
                .exceptionally(e -> {
                    // daca au aparut erori se propaga si se returneaza o lista de iteme goala
                    LOGGER.error("Error while processing final item list", e);
                    return new ArrayList<>();
                });
    }
}

