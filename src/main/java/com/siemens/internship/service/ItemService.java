package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

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

    @Async("taskExecutor")
    public CompletableFuture<Item> processSingleItem(Long id,
                                                     AtomicInteger processedCount,
                                                     AtomicInteger failedCount) {
        try {
            // se obtine item-ul din baza de data dupa id
            Optional<Item> optionalItem = itemRepository.findById(id);

            //daca este gasit se proceseaza
            if (optionalItem.isPresent()) {
                Item item = optionalItem.get();

                // item-ul se proceseaza si se salveaza inapoi in baza de date
                item.setStatus("PROCESSED");
                itemRepository.save(item);

                // se incrementeaza atomic numarul de elemente procesate
                processedCount.incrementAndGet();

                return CompletableFuture.completedFuture(item);
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

        return CompletableFuture.completedFuture(null);
    }

    @Async("taskExecutor")
    public CompletableFuture<List<Item>> processItemsAsync() {

        // in itemIds vor fi toate id-urile item-urilor din baza de date
        List<Long> itemIds = itemRepository.findAllIds();

        // aceasta lista contine toate task-urile (cate unul pentru fiecare id), pentru care se va astepta terminarea executiei
        List<CompletableFuture<Item>> futureThreadsList = new ArrayList<>();

        // counter atomic pentru a numara cate iteme au fost procesate cu succes (atomic pentru ca poate fi accesat de mai multe thread-uri)
        AtomicInteger processedCount = new AtomicInteger(0);

        // asemanator cu cel de sus doar ca pentru cele fara de succes
        AtomicInteger failedCount = new AtomicInteger(0);

        // se trece prin fiecare id
        for (Long id : itemIds) {
            // task-ul curent este adaugat in lista cu toate task-urile
            futureThreadsList.add(processSingleItem(id, processedCount, failedCount));
        }

        return CompletableFuture
                .allOf(futureThreadsList.toArray(new CompletableFuture[0])) // asa se asteapta terminarea tuturor task-urilor din lista
                .thenApply(x -> {   // dupa ce toate sunt gata se afiseaza cate au procesate in total, cate cu succes si cate nu
                    LOGGER.info("Processed successfully: " + processedCount.get() + " , failed : " + failedCount.get());

                    List<Item> itemsProcessed = new ArrayList<>();

                    // dupa incheierea tuturor task-urilor se incearca extragerea item-ului
                    // chiar daca nu s-a reusit extragerea item-ul tot a fost procesat, de aceea contoarele de processed si failed se modifica in cealalta functie
                    for(CompletableFuture<Item> futureThread : futureThreadsList) {
                        try {
                            Item item = futureThread.join();
                            if(item != null) {
                                itemsProcessed.add(item);
                            }
                        }
                        catch (Exception e) {
                            LOGGER.error("Error retrieving processed item from task");
                        }
                    }

                    // aici se returneaza lista de elemente procesate
                    return itemsProcessed;
                })
                .exceptionally(e -> {
                    // daca au aparut erori returneaza o lista de iteme goala
                    LOGGER.error("Error while processing final item list", e);
                    return new ArrayList<>();
                });
    }
}

