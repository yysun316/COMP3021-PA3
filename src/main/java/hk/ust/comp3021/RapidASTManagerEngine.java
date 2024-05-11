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
        int xmlIDsSize = xmlIDs.size();
        int iDPerThread = xmlIDsSize / numThread;
        int remainder = xmlIDsSize % numThread;
        List<Thread> threads = new ArrayList<>(numThread);

        for (int i = 0; i < numThread; i++) {
            int startIdx = i * iDPerThread;
            int endIdx = startIdx + iDPerThread;
            if (i == numThread - 1) {
                endIdx += remainder;
            }
            List<String> list = xmlIDs.subList(startIdx, endIdx);
            Thread thread = new Thread(() -> {
                list.forEach(id -> new ParserWorker(id, xmlDirPath, id2ASTModules).run());
            });
            threads.add(thread);
            thread.start();
        }
        joinAll(threads);
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
        HashMap<String, Object[]> loadCommands = new HashMap<>();
        HashMap<String, List<Object[]>> nonNodeCommands = new HashMap<>();
        ArrayList<Object[]> nodeCommands = new ArrayList<>();
        /* Separate the commands into 2 categories: load , query */
        classifyCommands(commands, loadCommands, nonNodeCommands, nodeCommands);
        /* Remove all commands without load */
        nonNodeCommands.entrySet().removeIf(entry -> !loadCommands.containsKey(entry.getKey()));
        nodeCommands.removeIf(x -> !loadCommands.containsKey((String) x[1]));
        /* Start running node commands if processedIds.size() == loadCommands.size() */
        ArrayList<Thread> threads = new ArrayList<>();
        loadCommands.values().forEach(command -> {
            Thread thread = new Thread(() -> {
                getParserWorker(command).run();
                synchronized (processedIds) {
                    processedIds.add((String) command[1]);
                    processedIds.notifyAll();
                }
            });
            threads.add(thread);
            thread.start();
        });

        ArrayList<QueryWorker> queryWorkers = new ArrayList<>(); /* For get result */
        nonNodeCommands.forEach((astID, commandList) -> {
            Thread thread = new Thread(() -> {
                synchronized (processedIds) {
                    while (!processedIds.contains(astID)) {
                        try {
                            processedIds.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                commandList.forEach(command -> {
                    QueryWorker worker = getQueryWorker(command, 1);
                    synchronized (queryWorkers) {
                        queryWorkers.add(worker);
                    }
                    worker.run();
                });
            });
            threads.add(thread);
            thread.start();
        });

        synchronized (processedIds) {
            while (!(processedIds.size() == loadCommands.size())) {
                try {
                    processedIds.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            nodeCommands.forEach(command -> {
                Thread thread = new Thread(() -> {
                    QueryWorker worker = getQueryWorker(command, 1);
                    synchronized (queryWorkers) {
                        queryWorkers.add(worker);
                    }
                    worker.run();
                });
                threads.add(thread);
                thread.start();
            });
        }
        joinAll(threads);
        return collectResult(queryWorkers, commands.size());
    }

    private List<Object> collectResult(ArrayList<QueryWorker> queryWorkers, int size) {
        for (QueryWorker queryWorker : queryWorkers) {
            System.out.println(queryWorker.queryName + queryWorker.queryID);
        }
        Object[] result = new Object[size];
        queryWorkers.forEach(worker -> {
            int idx = Integer.parseInt(worker.queryID) - 1;
            result[idx] = worker.getResult();
            System.out.println(worker.getResult());
        });
        allResults.addAll(Arrays.stream(result).filter(Objects::nonNull).toList());
//        allResults.forEach(System.out::println);
        return allResults;
    }

    private ParserWorker getParserWorker(Object[] command) {
        // command id, ast id, command name, command args
        return new ParserWorker((String) command[1], (String) (((Object[]) command[3])[0]), id2ASTModules);
    }

    private QueryWorker getQueryWorker(Object[] queryCommand, int mode) {
        // query id, ast id, query name, query args
        String queryId = (String) queryCommand[0];
        String astId = (String) queryCommand[1];
        String queryName = (String) queryCommand[2];
        Object[] args = (Object[]) queryCommand[3];
        return new QueryWorker(id2ASTModules, queryId, astId, queryName, args, mode);
    }

    private static void classifyCommands(List<Object[]> commands, HashMap<String, Object[]> loadCommands,
                                         HashMap<String, List<Object[]>> nonNodeCommands,
                                         ArrayList<Object[]> nodeCommands) {
        for (Object[] command : commands) {
            // command id, ast id, command name, command args
            // query id, ast id, query name, query args
            String astId = command[1].toString();
            String commandName = command[2].toString();
            if (commandName.equals("processXMLParsing")) {
                loadCommands.put(astId, command);
            } else if (QueryWorker.QUERY_NODES.contains(commandName)) {
                nodeCommands.add(command);
            } else {
                nonNodeCommands.computeIfAbsent(astId, k -> new ArrayList<>()).add(command);
            }
        }
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
        HashMap<String, Object[]> loadCommands = new HashMap<>();
        HashMap<String, List<Object[]>> nonNodeCommands = new HashMap<>();
        ArrayList<Object[]> nodeCommands = new ArrayList<>();
        /* Separate the commands into 2 categories: load , query */
        classifyCommands(commands, loadCommands, nonNodeCommands, nodeCommands);
        /* Remove all commands without load */
        nonNodeCommands.entrySet().removeIf(entry -> !loadCommands.containsKey(entry.getKey()));
        nodeCommands.removeIf(x -> !loadCommands.containsKey((String) x[1]));

        Thread loadThread = new Thread(() -> loadHelper(loadCommands, processedIds));
        loadThread.start();

        Thread queryThread = new Thread(() ->
                queryHelper(nonNodeCommands, nodeCommands, processedIds, loadCommands, commands));
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
        System.out.println(allResults);
        return allResults;
    }


    private List<Object> queryHelper(HashMap<String, List<Object[]>> nonNodeCommands,
                                     ArrayList<Object[]> nodeCommands,
                                     Set<String> processedIds,
                                     HashMap<String, Object[]> loadCommands,
                                     List<Object[]> commands) {
        ArrayList<QueryWorker> queryWorkers = new ArrayList<>();
        nonNodeCommands.forEach((astID, commandList) -> {
            synchronized (processedIds) {
                while (!processedIds.contains(astID)) {
                    try {
                        processedIds.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            commandList.forEach(command -> {
                QueryWorker worker = getQueryWorker(command, 0);
                queryWorkers.add(worker);
                worker.run();
            });
        });
        synchronized (processedIds) {
            while (!(processedIds.size() == loadCommands.size())) {
                try {
                    processedIds.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            nodeCommands.forEach(command -> {
                QueryWorker worker = getQueryWorker(command, 0);
                queryWorkers.add(worker);
                worker.run();
            });
        }
        return collectResult(queryWorkers, commands.size());
    }

    private void loadHelper(HashMap<String, Object[]> loadCommands, Set<String> processedIds) {
        loadCommands.values().forEach(command -> {
            getParserWorker(command).run();
            synchronized (processedIds) {
                processedIds.add((String) command[1]);
                processedIds.notifyAll();
            }
        });
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
        Set<String> processedIds = new HashSet<>(); /* Storing the processed astIDs */
        HashMap<String, Object[]> loadCommands = new HashMap<>();
        HashMap<String, List<Object[]>> nonNodeCommands = new HashMap<>();
        ArrayList<Object[]> nodeCommands = new ArrayList<>();
        /* Separate the commands into 2 categories: load , query */
        classifyCommands(commands, loadCommands, nonNodeCommands, nodeCommands);
        /* Remove all commands without load */
        nonNodeCommands.entrySet().removeIf(entry -> !loadCommands.containsKey(entry.getKey()));
        nodeCommands.removeIf(x -> !loadCommands.containsKey((String) x[1]));

        /* Create a fixed thread pool*/
        ExecutorService executor = Executors.newFixedThreadPool(numThread);

        /* Submit the load commands to the thread pool and store the Future objects */
        loadCommands.values().forEach(command -> {
            executor.submit(() -> {
                getParserWorker(command).run();
                synchronized (processedIds) {
                    processedIds.add((String) command[1]);
                    processedIds.notifyAll();
                }
            });
        });

        /* Wait for the load commands to finish and then submit the corresponding query commands */
        ArrayList<QueryWorker> queryWorkers = new ArrayList<>(); /* For get result */
        nonNodeCommands.forEach((astID, commandList) -> {
            executor.submit(() -> {
                synchronized (processedIds) {
                    while (!processedIds.contains(astID)) {
                        try {
                            processedIds.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                commandList.forEach(command -> {
                    QueryWorker worker = getQueryWorker(command, 0);
                    synchronized (queryWorkers) {
                        queryWorkers.add(worker);
                    }
                    worker.run();
                });
            });
        });

        synchronized (processedIds) {
            while (!(processedIds.size() == loadCommands.size())) {
                try {
                    processedIds.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            nodeCommands.forEach(command -> {
                executor.submit(() -> {
                    QueryWorker worker = getQueryWorker(command, 0);
                    synchronized (queryWorkers) {
                        queryWorkers.add(worker);
                    }
                    worker.run();
                });
            });
        }
        /* Shutdown the executor and wait for all tasks to finish */
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return collectResult(queryWorkers, commands.size());
    }
}
