package hk.ust.comp3021;

import hk.ust.comp3021.parallel.*;
import hk.ust.comp3021.utils.*;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


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
        for (String xmlID : xmlIDs) {
            service.execute(new ParserWorker(xmlID, xmlDirPath, id2ASTModules));
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
     *                   Hint3: please distribute the files to be loaded for each thread manually
     *                   and try to achieve high efficiency
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
            workers.add(new QueryWorker(id2ASTModules, (String) command[0],
                    (String) command[1], (String) command[2],
                    (Object[]) command[3], executionMode));
        }
        switch (executionMode) {
            case 0 -> executeCommandsSerial(workers);
            case 1 -> executeCommandsParallel(workers);
            case 2 -> executeCommandsParallelWithOrder(workers);
            default -> System.out.println("Invalid execution mode");
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
        joinAll(threads);

    }

    /**
     * TODO: Implement `executeCommandsParallelWithOrder` to handle a list of `QueryWorker`
     *
     * @param workers a list of workers that should be executed in parallel with correct order
     *                <p>
     *                Hint1: you can invoke {@link RapidASTManagerEngine#executeCommandsParallel(List)} to
     *                reuse its logic
     *                Hint2: you can use unlimited number of threads
     *                Hint3: please design the order of queries running in parallel based on the calling
     *                dependence of method
     *                in queryOnClass
     */
    private void executeCommandsParallelWithOrder(List<QueryWorker> workers) {
        /* If we cannot find the prerequisite using his astID + queryName, meaning they are fulfilled */
        HashMap<String, Integer> prerequisites = new HashMap<>(); /* astID + queryName to fulfilled */
        ArrayList<Thread> threads = new ArrayList<>();
        for (QueryWorker worker : workers) {
            if (worker.queryName.equals("findSuperClasses")) {/* haveSuperClass / findAllMethods cares the class name*/
                String key1 = worker.astID + worker.queryName + worker.args[0].toString();
                String key2 = worker.astID + worker.queryName;
                prerequisites.put(key1, prerequisites.getOrDefault(key1, 0) + 1);
                prerequisites.put(key2, prerequisites.getOrDefault(key2, 0) + 1);
            } else {
                String key = worker.astID + worker.queryName;
                prerequisites.put(key, prerequisites.getOrDefault(key, 0) + 1);
            }
        }
        QueryWorker.setPrerequisites(prerequisites);
        for (QueryWorker worker : workers) {
            Thread thread = new Thread(worker);
            thread.start();
            threads.add(thread);
        }
        joinAll(threads);
    }

    private static void joinAll(List<Thread> threads) {
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
     *                 Hint3: please design the order of commands, where for specific ID,
     *                 AST load should be executed before query
     *                 Hint4: threads would write into/read from {@link RapidASTManagerEngine#id2ASTModules} at
     *                 the same time, please
     *                 synchronize them carefully
     *                 Hint5: you can invoke {@link QueryWorker#run()} and {@link ParserWorker#run()}
     *                 Hint6: order of queries should be consistent to that in given commands, no need to consider
     *                 redundant computation now
     */
    public List<Object> processCommandsInterLeaved(List<Object[]> commands) {
        Set<String> processedIds = new HashSet<>(); /* Storing the processed astIDs */
        List<Object[]> loadCommands = new ArrayList<>();
        List<Object[]> queryCommands = new ArrayList<>();
        /* Separate the commands into 2 categories: load , query */
        for (Object[] command : commands) {
            if (command[2].equals("processXMLParsing")) {
                loadCommands.add(command);
            } else {
                queryCommands.add(command);
            }
        }

        Thread loadThread = new Thread(() -> loadUnlimitedHelper(loadCommands, processedIds));
        loadThread.start();


        Thread queryThread = new Thread(() ->
                allResults.addAll(queryUnlimitedHelper(queryCommands, processedIds, commands.size())));
        queryThread.start();

        try {
            loadThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            queryThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finishedProcessing = false;
        return allResults;
    }

    private void loadUnlimitedHelper(List<Object[]> loadCommands, Set<String> processedIds) {
        int loadCount = 0;
        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<ParserWorker> parserWorkers = new ArrayList<>();

        /* Submit jobs */
        while (loadCount < loadCommands.size()) {
            // command id, ast id, command name, command args
            Object[] loadCommand = loadCommands.get(loadCount);
            String xmlID = (String) loadCommand[1];
            String xmlDirPath = (String) (((Object[]) loadCommand[3])[0]);
            ParserWorker parserWorker = new ParserWorker(xmlID, xmlDirPath, id2ASTModules);
            Thread thread = new Thread(parserWorker);
            thread.start();
            threads.add(thread);
            parserWorkers.add(parserWorker);
            loadCount++;
        }

        /* Get all the processedIds*/
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            LOCK.lock();
            processedIds.add(parserWorkers.get(i).getXmlID());
            LOCK.unlock();
        }
        /* finished processing, no more update will be done*/
        LOCK.lock();
        finishedProcessing = true;
        LOCK.unlock();
    }

    private List<Object> queryUnlimitedHelper(List<Object[]> queryCommands, Set<String> processedIds, int size) {
        int queryCount = queryCommands.size();
        QueryWorker[] queryWorkers = new QueryWorker[size];
        ArrayList<Thread> threads = new ArrayList<>();

        while (queryCount > 0 && !finishedProcessing) {
            // query id, ast id, query name, query args
            Iterator<Object[]> iterator = queryCommands.iterator();
            while (iterator.hasNext()) {
                Object[] queryCommand = iterator.next();
                String astId = (String) queryCommand[1];
                LOCK.lock();
                if (processedIds.contains(astId)) {
                    String queryId = (String) queryCommand[0];
                    String queryName = (String) queryCommand[2];
                    Object[] args = (Object[]) queryCommand[3];
                    QueryWorker worker = new QueryWorker(id2ASTModules, queryId,
                            astId, queryName, args, 1);
                    Thread thread = new Thread(worker);
                    thread.start();
                    threads.add(thread);
                    queryWorkers[Integer.parseInt(queryId) - 1] = worker; /* QueryWorkers at their query ID */
                    iterator.remove(); /* Be careful */
                    queryCount--;
                }
                LOCK.unlock();
            }
        }
        /* Check it one more time to ensure all the commands are processed */
        Iterator<Object[]> iterator = queryCommands.iterator();
        while (iterator.hasNext()) {
            Object[] queryCommand = iterator.next();
            String astId = (String) queryCommand[1];
            LOCK.lock();
            if (processedIds.contains(astId)) {
                String queryId = (String) queryCommand[0];
                String queryName = (String) queryCommand[2];
                Object[] args = (Object[]) queryCommand[3];
                QueryWorker worker = new QueryWorker(id2ASTModules, queryId, astId, queryName, args, 1);
                Thread thread = new Thread(worker);
                thread.start();
                threads.add(thread);
                queryWorkers[Integer.parseInt(queryId) - 1] = worker; /* QueryWorkers at their query ID */
                iterator.remove(); /* Be careful */
                queryCount--;
            }
            LOCK.unlock();
        }

        joinAll(threads);

        /* Retrieve result */
        return Arrays.stream(queryWorkers).
                filter(Objects::nonNull).
                map(QueryWorker::getResult).
                collect(Collectors.toList());
    }

    /**
     * TODO: Implement `processCommandsInterLeavedTwoThread` to handle a list of commands
     *
     * @param commands a list of import and query commands that should be executed in parallel
     *                 <p>
     *                 Hint1: you can **only** use {@link Thread} to create threads
     *                 Hint2: you can only use two threads, one for AST load, another for query
     *                 Hint3: please design the order of commands, where for specific ID, AST load
     *                 should be executed before query
     *                 Hint4: threads would write into/read from {@link RapidASTManagerEngine#id2ASTModules}
     *                 at the same time, please
     *                 synchronize them carefully
     *                 Hint5: you can invoke {@link QueryWorker#run()} and {@link ParserWorker#run()}
     *                 Hint6: order of queries should be consistent to that in given commands, no need to consider
     *                 redundant computation now
     */
    public List<Object> processCommandsInterLeavedTwoThread(List<Object[]> commands) {
        Set<String> processedIds = new HashSet<>(); /* Storing the processed astIDs */
        List<Object[]> loadCommands = new ArrayList<>();
        List<Object[]> queryCommands = new ArrayList<>();
        /* Separate the commands into 2 categories: load , query */
        for (Object[] command : commands) {
            if (command[2].equals("processXMLParsing")) {
                loadCommands.add(command);
            } else {
                queryCommands.add(command);
            }
        }

        Thread loadThread = new Thread(() -> loadHelper(loadCommands, processedIds));
        loadThread.start();
        Thread queryThread = new Thread(() ->
                allResults.addAll(queryHelper(queryCommands, processedIds, commands.size())));
        queryThread.start();
        try {
            loadThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            queryThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finishedProcessing = false;
        return allResults;
    }

    private volatile boolean finishedProcessing = false;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private List<Object> queryHelper(List<Object[]> queryCommands, Set<String> processedIds, int size) {
        int queryCount = queryCommands.size();
        QueryWorker[] queryWorkers = new QueryWorker[size];
        while (queryCount > 0 && !finishedProcessing) {
            // query id, ast id, query name, query args
            Iterator<Object[]> iterator = queryCommands.iterator();
            while (iterator.hasNext()) {
                Object[] queryCommand = iterator.next();
                String astId = (String) queryCommand[1];
                LOCK.lock();
                if (processedIds.contains(astId)) {
                    String queryId = (String) queryCommand[0];
                    String queryName = (String) queryCommand[2];
                    Object[] args = (Object[]) queryCommand[3];
                    QueryWorker worker = new QueryWorker(id2ASTModules, queryId, astId, queryName, args, 0);
                    worker.run();
                    queryWorkers[Integer.parseInt(queryId) - 1] = worker; /* QueryWorkers at their query ID */
                    iterator.remove(); /* Be careful */
                    queryCount--;
                }
                LOCK.unlock();
            }
        }
        /* Check it one more time to ensure all the commands are processed */
        Iterator<Object[]> iterator = queryCommands.iterator();
        while (iterator.hasNext()) {
            Object[] queryCommand = iterator.next();
            String astId = (String) queryCommand[1];
            LOCK.lock();
            if (processedIds.contains(astId)) {
                String queryId = (String) queryCommand[0];
                String queryName = (String) queryCommand[2];
                Object[] args = (Object[]) queryCommand[3];
                QueryWorker worker = new QueryWorker(id2ASTModules, queryId, astId, queryName, args, 0);
                worker.run();
                queryWorkers[Integer.parseInt(queryId) - 1] = worker; /* QueryWorkers at their query ID */
                iterator.remove(); /* Be careful */
                queryCount--;
            }
            LOCK.unlock();
        }
        /* Retrieve result */
        return Arrays.stream(queryWorkers).
                filter(Objects::nonNull).
                map(QueryWorker::getResult).
                collect(Collectors.toList());
    }

    private void loadHelper(List<Object[]> loadCommands, Set<String> processedIds) {
        int loadCount = 0;
        ParserWorker parserWorker = null;
        while (loadCount < loadCommands.size()) {
            if (parserWorker != null) { /* Finish parsing and we can execute its query for this id */
                LOCK.lock();
                processedIds.add(parserWorker.getXmlID());
                LOCK.unlock();
            }
            // command id, ast id, command name, command args
            Object[] loadCommand = loadCommands.get(loadCount);
            String xmlID = (String) loadCommand[1];
            String xmlDirPath = (String) (((Object[]) loadCommand[3])[0]);
            parserWorker = new ParserWorker(xmlID, xmlDirPath, id2ASTModules);
            parserWorker.run(); /* only two threads */
            loadCount++;
        }

        if (parserWorker != null) {
            LOCK.lock();
            processedIds.add(parserWorker.getXmlID());
            LOCK.unlock();
        }
        /* finished processing, no more update will be done*/
        LOCK.lock();
        finishedProcessing = true;
        LOCK.unlock();
    }


    /**
     * TODO: (Bonus) Implement `processCommandsInterLeavedTwoThread` to handle a list of commands
     *
     * @param commands  a list of import and query commands that should be executed in parallel
     * @param numThread number of threads you are allowed to use
     *                  <p>
     *                  Hint1: you can only distribute commands on your need
     *                  Hint2: please design the order of commands, where for specific ID,
     *                  AST load should be executed before query
     *                  Hint3: threads would write into/read from {@link RapidASTManagerEngine#id2ASTModules}
     *                  at the same time, please
     *                  synchronize them carefully
     *                  Hint4: you can invoke {@link QueryWorker#run()} and {@link ParserWorker#run()}
     */
    @SuppressWarnings("unchecked")
    public List<Object> processCommandsInterLeavedFixedThread(List<Object[]> commands, int numThread) {
        // TODO: Bonus: interleaved parsing and query with given number of threads
        // TODO: separate parser tasks and query tasks with the goal of efficiency
        // Separate the commands into load and query commands
        Map<String, Object[]> loadCommands = new HashMap<>();
        Map<String, List<Object[]>> queryCommands = new HashMap<>();
        for (Object[] command : commands) {
            if (command[2].equals("processXMLParsing")) {
                loadCommands.put((String) command[1], command);
            } else {
                queryCommands.computeIfAbsent((String) command[1], k -> new ArrayList<>()).add(command);
            }
        }

        /* Create a fixed thread pool*/
        ExecutorService executor = Executors.newFixedThreadPool(numThread);

        /* Submit the load commands to the thread pool and store the Future objects */
        Map<String, Future<?>> loadFutures = new HashMap<>();
        for (Map.Entry<String, Object[]> entry : loadCommands.entrySet()) {
            String xmlID = (String) entry.getValue()[1];
            String xmlDirPath = (String) (((Object[]) entry.getValue()[3])[0]);
            ParserWorker parserWorker = new ParserWorker(xmlID, xmlDirPath, id2ASTModules);
            Future<?> future = executor.submit(parserWorker);
            loadFutures.put(entry.getKey(), future);
        }

        /* Wait for the load commands to finish and then submit the corresponding query commands */
        Map<QueryWorker, Future<Object>> futureTasks = new HashMap<>();
        int countDone = 0;
        while (countDone < loadFutures.size()) {
            for (Map.Entry<String, List<Object[]>> entry : queryCommands.entrySet()) {
                if (entry.getValue() == null)
                    continue;
                Future<?> loadFuture = loadFutures.get(entry.getKey());
                if (loadFuture.isDone()) {
                    for (Object[] queryCommand : entry.getValue()) {
                        String queryId = (String) queryCommand[0];
                        String queryName = (String) queryCommand[2];
                        Object[] args = (Object[]) queryCommand[3];
                        QueryWorker queryWorker = new QueryWorker(id2ASTModules, queryId, entry.getKey(),
                                queryName, args, 0);
                        futureTasks.put(queryWorker, (Future<Object>) executor.submit(queryWorker));
                    }
                    queryCommands.put(entry.getKey(), null);
                    countDone++;
                }
            }
        }
        /* Shutdown the executor and wait for all tasks to finish */
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Object[] results = new Object[commands.size()];
        for (Map.Entry<QueryWorker, Future<Object>> futureEntry : futureTasks.entrySet()) {
            try {
                futureEntry.getValue().get(); // wait
                Object o = futureEntry.getKey().getResult();
                int pos = Integer.parseInt(futureEntry.getKey().queryID) - 1;
                results[pos] = o;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        allResults.addAll(Arrays.stream(results).filter(Objects::nonNull).toList());
        return allResults;
    }
}
