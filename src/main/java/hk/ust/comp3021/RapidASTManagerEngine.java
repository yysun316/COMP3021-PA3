package hk.ust.comp3021;

import hk.ust.comp3021.parallel.*;
import hk.ust.comp3021.utils.*;

import java.util.concurrent.*;
import java.util.*;


public class RapidASTManagerEngine {
    private final HashMap<String, ASTModule> id2ASTModules = new HashMap<>();
    private final List<Object> allResults = new ArrayList<>();

    public HashMap<String, ASTModule> getId2ASTModule() {
        return id2ASTModules;
    }

    public List<Object> getAllResults() {
        return allResults;
    }

    /**
     * TODO: Implement `processXMLParsingPool` to load a list of XML files in parallel
     *
     * @param xmlDirPath the directory of XML files to be loaded
     * @param xmlIDs     a list of XML file IDs
     * @param numThread  the number of threads you are allowed to use
     *                   <p>
     *                   Hint1: you can use thread pool {@link ExecutorService} to implement the method
     *                   Hint2: you can use {@link ParserWorker#run()}
     */

    public void processXMLParsingPool(String xmlDirPath, List<String> xmlIDs, int numThread) {
        ExecutorService service = Executors.newFixedThreadPool(numThread);
        for (int i = 0; i < xmlIDs.size(); i++) {
            service.execute(new ParserWorker(xmlIDs.get(i), xmlDirPath, id2ASTModules));
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO: Implement `processXMLParsingDivide` to load a list of XML files in parallel
     *
     * @param xmlDirPath the directory of XML files to be loaded
     * @param xmlIDs     a list of XML file IDs
     * @param numThread  the number of threads you are allowed to use
     *                   <p>
     *                   Hint1: you can **only** use {@link Thread} to implement the method
     *                   Hint2: you can use {@link ParserWorker#run()}
     *                   Hint3: please distribute the files to be loaded for each thread manually and try to achieve high efficiency
     */
    public void processXMLParsingDivide(String xmlDirPath, List<String> xmlIDs, int numThread) {
        Thread[] threads = new Thread[numThread];
        int count = 0;
        while (count < xmlIDs.size()) {
            for (int i = 0; i < numThread; i++) {
                ParserWorker worker = new ParserWorker(xmlIDs.get(count), xmlDirPath, id2ASTModules);
                Thread thread = new Thread(worker);
                processXMLParsingDivideHelper(threads, thread);
                thread.start();
                count++;
            }
        }
        for (int i = 0; i < numThread; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /***
     * A helper which finds a space in threads for a new thread to be inserted
     * @param threads the threads array
     * @param thread the new thread to be inserted
     */
    private void processXMLParsingDivideHelper(Thread[] threads, Thread thread) {
        while (true) {
            for (int i = 0; i < threads.length; i++) {
                if (threads[i] == null || !threads[i].isAlive()) {
                    threads[i] = thread;
                    return;
                }
            }
        }
    }


    /**
     * TODO: Implement `processCommands` to conduct a list of queries on ASTs based on execution mode
     *
     * @param commands      a list of queries, you can refer to test cases to learn its format
     * @param executionMode mode 0 to mode 2
     *                      <p>
     *                      Hint1: you need to invoke {@link RapidASTManagerEngine#executeCommandsSerial(List)}
     *                      {@link RapidASTManagerEngine#executeCommandsParallel(List)}
     *                      and {@link RapidASTManagerEngine#executeCommandsParallelWithOrder(List)}
     */
    public List<Object> processCommands(List<Object[]> commands, int executionMode) {
        List<QueryWorker> workers = new ArrayList<>();
        for (Object[] command : commands) {
            workers.add(new QueryWorker(id2ASTModules, command[0].toString(),
                    command[1].toString(), command[2].toString(),
                    (Object[]) command[3], executionMode));
        }
        switch (executionMode){
            case 0 -> executeCommandsSerial(workers);
            case 1 -> executeCommandsParallel(workers);
            case 2 -> executeCommandsParallelWithOrder(workers);
        }
        workers.forEach(worker -> allResults.add(worker.getResult()));
        return allResults;
    }

    /**
     * TODO: Implement `executeCommandsSerial` to handle a list of `QueryWorker`
     *
     * @param workers a list of workers that should be executed sequentially
     */
    private void executeCommandsSerial(List<QueryWorker> workers) {
        for (QueryWorker worker : workers) {
            worker.run(); /* Sequential */
        }
    }

    /**
     * TODO: Implement `executeCommandsParallel` to handle a list of `QueryWorker`
     *
     * @param workers a list of workers that should be executed in parallel
     *                <p>
     *                Hint1: you can **only** use {@link Thread} to implement the method
     *                Hint2: you can use unlimited number of threads
     */
    private void executeCommandsParallel(List<QueryWorker> workers) {
        List<Thread> threads = new ArrayList<>();
        workers.forEach(worker -> {
            Thread thread = new Thread(worker);
            thread.start();
            threads.add(thread);
        });
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * TODO: Implement `executeCommandsParallelWithOrder` to handle a list of `QueryWorker`
     *
     * @param workers a list of workers that should be executed in parallel with correct order
     *                <p>
     *                Hint1: you can invoke {@link RapidASTManagerEngine#executeCommandsParallel(List)} to reuse its logic
     *                Hint2: you can use unlimited number of threads
     *                Hint3: please design the order of queries running in parallel based on the calling dependence of method
     *                in queryOnClass
     */
    private void executeCommandsParallelWithOrder(List<QueryWorker> workers) {
        Set<QueryWorker> visited = new HashSet<>();
        List<Thread> threads = new ArrayList<>();
        /* First execute all the find super classes method call */
        for (int i = 0; i < workers.size(); i++) {
            if (workers.get(i).queryName.equals("findSuperClasses")){
                Thread thread = new Thread(workers.get(i));
                threads.add(thread);
                thread.start();
                visited.add(workers.get(i));
            }
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /* Then execute all the non findClassesWithMain methods */
        for (int i = 0; i < workers.size(); i++)
        {
            if (!workers.get(i).queryName.equals("findClassesWithMain") && !visited.contains(workers.get(i)))
            {
                Thread thread = new Thread(workers.get(i));
                threads.add(thread);
                thread.start();
                visited.add(workers.get(i));
            }
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /* Lastly execute the findClassesWithMain method */
        for (int i = 0; i < workers.size(); ++i)
        {
            if (workers.get(i).queryName.equals("findClassesWithMain"))
            {
                Thread thread = new Thread(workers.get(i));
                threads.add(thread);
                thread.start();
                visited.add(workers.get(i));
            }
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * TODO: Implement `processCommandsInterLeaved` to handle a list of commands
     *
     * @param commands a list of import and query commands that should be executed in parallel
     *                 <p>
     *                 Hint1: you can **only** use {@link Thread} to create threads
     *                 Hint2: you can use unlimited number of threads
     *                 Hint3: please design the order of commands, where for specific ID, AST load should be executed before query
     *                 Hint4: threads would write into/read from {@link RapidASTManagerEngine#id2ASTModules} at the same time, please
     *                 synchronize them carefully
     *                 Hint5: you can invoke {@link QueryWorker#run()} and {@link ParserWorker#run()}
     *                 Hint6: order of queries should be consistent to that in given commands, no need to consider
     *                 redundant computation now
     */
    public List<Object> processCommandsInterLeaved(List<Object[]> commands) {

        return allResults;
    }


    /**
     * TODO: Implement `processCommandsInterLeavedTwoThread` to handle a list of commands
     *
     * @param commands a list of import and query commands that should be executed in parallel
     *                 <p>
     *                 Hint1: you can **only** use {@link Thread} to create threads
     *                 Hint2: you can only use two threads, one for AST load, another for query
     *                 Hint3: please design the order of commands, where for specific ID, AST load should be executed before query
     *                 Hint4: threads would write into/read from {@link RapidASTManagerEngine#id2ASTModules} at the same time, please
     *                 synchronize them carefully
     *                 Hint5: you can invoke {@link QueryWorker#run()} and {@link ParserWorker#run()}
     *                 Hint6: order of queries should be consistent to that in given commands, no need to consider
     *                 redundant computation now
     */
    public List<Object> processCommandsInterLeavedTwoThread(List<Object[]> commands) {

        return allResults;
    }

    /**
     * TODO: (Bonus) Implement `processCommandsInterLeavedTwoThread` to handle a list of commands
     *
     * @param commands  a list of import and query commands that should be executed in parallel
     * @param numThread number of threads you are allowed to use
     *                  <p>
     *                  Hint1: you can only distribute commands on your need
     *                  Hint2: please design the order of commands, where for specific ID, AST load should be executed before query
     *                  Hint3: threads would write into/read from {@link RapidASTManagerEngine#id2ASTModules} at the same time, please
     *                  synchronize them carefully
     *                  Hint4: you can invoke {@link QueryWorker#run()} and {@link ParserWorker#run()}
     */
    public List<Object> processCommandsInterLeavedFixedThread(List<Object[]> commands, int numThread) {
        // TODO: Bonus: interleaved parsing and query with given number of threads
        // TODO: separate parser tasks and query tasks with the goal of efficiency
        return allResults;
    }
}
