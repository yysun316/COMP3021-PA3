package hk.ust.comp3021.parallel;

import hk.ust.comp3021.query.*;
import hk.ust.comp3021.utils.*;

import java.util.*;
import java.util.concurrent.*;

public class QueryWorker implements Runnable {
    public HashMap<String, ASTModule> id2ASTModules;
    public String queryID;
    public String astID;
    public String queryName;
    public Object[] args;
    public int mode;
    private Object result;

    /* Lists storing the name of functions in the query classes */
    public static final ArrayList<String> queryMethods;
    public static final ArrayList<String> queryClasses;
    public static final ArrayList<String> queryNodes;

    static {
        queryMethods = new ArrayList<>(Arrays.asList("findEqualCompareInFunc", "findFuncWithBoolParam", "findUnusedParamInFunc", "findDirectCalledOtherB", "answerIfACalledB"));
        queryClasses = new ArrayList<>(Arrays.asList("findSuperClasses", "haveSuperClass", "findOverridingMethods", "findAllMethods", "findClassesWithMain"));
        queryNodes = new ArrayList<>(Arrays.asList("findFuncWithArgGtN", "calculateOp2Nums", "calculateNode2Nums", "processNodeFreq"));
    }


    public QueryWorker(HashMap<String, ASTModule> id2ASTModules, String queryID, String astID, String queryName, Object[] args, int mode) {
        this.id2ASTModules = id2ASTModules;
        this.queryID = queryID;
        this.astID = astID;
        this.queryName = queryName;
        this.args = args;
        this.mode = mode;
    }

    public Object getResult() {
        return result;
    }

    public void run() {
        if (mode == 0) {
            runSerial();
        } else if (mode == 1) {
            runParallel();
        } else if (mode == 2) {
            runParallelWithOrder();
        }
    }

    /**
     * TODO: Implement `runSerial` to process current query command and store the results in `result`
     * <p>
     * Hint1: you must invoke the methods in {@link QueryOnNode}, {@link QueryOnMethod} and {@link QueryOnClass}
     * to achieve the query
     */
    private void runSerial() {
        /* Determine which type of query it is */
        if (queryMethods.contains(queryName)) {
            handleMethods();
        } else if (queryNodes.contains(queryName)) {
            handleNodes();
        } else if (queryClasses.contains(queryName)) {
            handleClasses();
        }

    }

    private void handleMethods() {
        QueryOnMethod queryOnMethod = new QueryOnMethod(id2ASTModules.get(astID));
        result = switch (queryName) {
            case "findEqualCompareInFunc" -> queryOnMethod.findEqualCompareInFunc.apply((String) args[0]);
            case "findFuncWithBoolParam" -> queryOnMethod.findFuncWithBoolParam.get();
            case "findUnusedParamInFunc" -> queryOnMethod.findUnusedParamInFunc.apply((String) args[0]);
            case "findDirectCalledOtherB" -> queryOnMethod.findDirectCalledOtherB.apply((String) args[0]);
            case "answerIfACalledB" -> queryOnMethod.answerIfACalledB.test((String) args[0], (String) args[1]);
            default -> null;
        };
        if (result == null) System.out.println("Check handleMethods");
    }

    private void handleNodes() {
        QueryOnNode queryonNode = new QueryOnNode(id2ASTModules);
        result = switch (queryName) {
            case "findFuncWithArgGtN" -> queryonNode.findFuncWithArgGtN.apply((int) args[0]);
            case "calculateOp2Nums" -> queryonNode.calculateOp2Nums.get();
            case "calculateNode2Nums" -> queryonNode.calculateNode2Nums.apply((String) args[0]);
            case "processNodeFreq" -> queryonNode.processNodeFreq.get();
            default -> null;
        };
        if (result == null) System.out.println("Check handleNodes");
    }

    private void handleClasses() {
        QueryOnClass queryOnClass = new QueryOnClass(id2ASTModules.get(astID));
        result = switch (queryName) {
            case "findSuperClasses" -> queryOnClass.findSuperClasses.apply((String) args[0]);
            case "haveSuperClass" -> queryOnClass.haveSuperClass.apply((String) args[0], (String) args[1]);
            case "findOverridingMethods" -> queryOnClass.findOverridingMethods.get();
            case "findAllMethods" -> queryOnClass.findAllMethods.apply((String) args[0]);
            case "findClassesWithMain" -> queryOnClass.findClassesWithMain.get();
            default -> null;
        };
        if (result == null) System.out.println("check handleClasses");
    }


    /**
     * TODO: Implement `runParallel` to process current query command and store the results in `result` where
     * queryOnNode should be conducted with multiple threads
     * <p>
     * Hint1: you must invoke the methods in {@link QueryOnNode}, {@link QueryOnMethod} and {@link QueryOnClass}
     * to achieve the query
     * Hint2: you can let methods in queryOnNode to work on single AST by changing the arguments when creating
     * {@link QueryOnNode} object
     * Hint3: please use {@link Thread} to achieve multi-threading
     * Hint4: you can invoke {@link QueryWorker#runSerial()} to reuse its logic
     */
    private void runParallel() {
        if (queryMethods.contains(queryName)) {
            handleMethods();
        } else if (queryClasses.contains(queryName)) {
            handleClasses();
        } else if (queryNodes.contains(queryName)) {
            handleNodesInParallel();
        }
    }

    private void handleNodesInParallel() {
        ArrayList<QueryWorker> workers = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();

        id2ASTModules.forEach((key, value) -> {
            HashMap<String, ASTModule> tmpMap = new HashMap<>();
            tmpMap.put(key, value);
            QueryWorker worker = new QueryWorker(tmpMap, queryID, astID, queryName, args, 0);
            Thread thread = new Thread(worker);
            thread.start();
            threads.add(thread);
            workers.add(worker);
        });
        /* Wait for termination */
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        /* Result retrieval */
        getResult(workers);

   /*     ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Object>> futures = new ArrayList<>();

        id2ASTModules.forEach((key, value) -> {
            HashMap<String, ASTModule> tmpMap = new HashMap<>();
            tmpMap.put(key, value);
            QueryWorker tmpWorker = new QueryWorker(tmpMap, queryID, astID, queryName, args, 0);
            futures.add((Future<Object>) executor.submit(tmpWorker));
        });

        result = switch (queryName) {
            case "findFuncWithArgGtN" -> {
                List<String> res = new ArrayList<>();
                futures.forEach(future -> {
                    try {
                        res.addAll((Collection<? extends String>) future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                yield res;
            }
            case "calculateOp2Nums" -> {
                HashMap<String, Integer> res = new HashMap();
                futures.forEach(future -> {
                    try {
                        res.putAll((Map<? extends String, ? extends Integer>) future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                yield res;
            }
            case "calculateNode2Nums" -> {
                Map<String, Long> res = new HashMap();
                futures.forEach(future -> {
                    try {
                        res.putAll((Map<? extends String, ? extends Long>) future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                yield res;
            }
            case "processNodeFreq" -> {
                List<Map.Entry<String, Integer>> res = new ArrayList<>();
                futures.forEach(future -> {
                    try {
                        res.addAll((Collection<? extends Map.Entry<String, Integer>>) future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });
                yield res;
            }
            default -> null;
        };*/
    }

    private void getResult(ArrayList<QueryWorker> workers) {
        ArrayList<Object> res = new ArrayList<>();
        workers.forEach(worker -> res.add(worker.getResult()));
        if ("findFuncWithArgGtN".equals(queryName)) result = res.stream().map(val -> (int) val).reduce(0, Integer::sum);
        if ("calculateOp2Nums".equals(queryName)) {
            HashMap<String, Integer> allMaps = new HashMap<>();
            res.stream().map(m -> (HashMap<String, Integer>) m)
                    .forEach(map ->
                            map.forEach((key, value) ->
                                    allMaps.put(key, allMaps.getOrDefault(key, 0) + value)));
            result = allMaps;
        }
        if ("calculateNode2Nums".equals(queryName)){
            Map<String, Long> allMaps = new HashMap<>();
            res.stream().map(m -> (HashMap<String, Long>) m)
                    .forEach(map ->
                            map.forEach((key, value) ->
                                    allMaps.put(key, allMaps.getOrDefault(key, 0L) + value)));
            result = allMaps;
        }
        if ("processNodeFreq".equals(queryName)){
            List<Map.Entry<String, Integer>> list = new ArrayList<>();
            res.forEach(l -> list.addAll((List<Map.Entry<String, Integer>>) l));
            result = list;
        }
    }


    /**
     * TODO: Implement `runParallelWithOrder` to process current query command and store the results in `result` where
     * the current query should wait until the prerequisite has been computed
     * <p>
     * Hint1: you must invoke the methods in {@link QueryOnNode}, {@link QueryOnMethod} and {@link QueryOnClass}
     * to achieve the query
     * Hint2: you can invoke {@link QueryWorker#runParallel()} to reuse its logic
     * Hint3: please use {@link Thread} to achieve multi-threading
     * Hint4: you can add new methods or fields in current class
     */
    private void runParallelWithOrder() {
        runParallel();
    }
}
