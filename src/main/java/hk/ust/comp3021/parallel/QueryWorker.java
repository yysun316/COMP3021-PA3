package hk.ust.comp3021.parallel;

import hk.ust.comp3021.query.*;
import hk.ust.comp3021.utils.*;

import java.util.*;
import java.util.stream.Collectors;

public class QueryWorker implements Runnable {
    public HashMap<String, ASTModule> id2ASTModules;
    public String queryID;
    public String astID;
    public String queryName;
    public Object[] args;
    public int mode;
    private Object result;

    /* Lists storing the name of functions in the query classes */
    public static final HashSet<String> QUERY_METHODS;
    public static final HashSet<String> QUERY_CLASSES;
    public static final HashSet<String> QUERY_NODES;

    static {
        QUERY_METHODS = new HashSet<>(Arrays.asList(
                "findEqualCompareInFunc", "findFuncWithBoolParam", "findUnusedParamInFunc",
                "findDirectCalledOtherB", "answerIfACalledB"));
        QUERY_CLASSES = new HashSet<>(Arrays.asList(
                "findSuperClasses", "haveSuperClass", "findOverridingMethods",
                "findAllMethods", "findClassesWithMain"));
        QUERY_NODES = new HashSet<>(Arrays.asList(
                "findFuncWithArgGtN", "calculateOp2Nums",
                "calculateNode2Nums", "processNodeFreq"));
    }


    public QueryWorker(HashMap<String, ASTModule> id2ASTModules, String queryID, String astID,
                       String queryName, Object[] args, int mode) {
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
        if (QUERY_METHODS.contains(queryName)) {
            handleMethods();
        } else if (QUERY_NODES.contains(queryName)) {
            handleNodes();
        } else if (QUERY_CLASSES.contains(queryName)) {
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
        if (QUERY_METHODS.contains(queryName)) {
            handleMethods();
        } else if (QUERY_CLASSES.contains(queryName)) {
            handleClasses();
        } else if (QUERY_NODES.contains(queryName)) {
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
    }

    @SuppressWarnings("unchecked")
    private void getResult(ArrayList<QueryWorker> workers) {
        if ("findFuncWithArgGtN".equals(queryName)) {
            result = workers.stream().map(QueryWorker::getResult)
                    .filter(list -> !((List<String>) list).isEmpty())
                    .flatMap(list -> ((List<String>) list).stream())
                    .collect(Collectors.toList());
        }
        if ("calculateOp2Nums".equals(queryName)) {
            result = workers.stream()
                    .map(QueryWorker::getResult)
                    .flatMap(result -> ((HashMap<String, Integer>) result).entrySet().stream())
                    .collect(HashMap<String, Integer>::new,
                            (m, e) -> m.merge(e.getKey(), e.getValue(), Integer::sum),
                            Map::putAll);
        }
        if ("calculateNode2Nums".equals(queryName)) {
            result = workers.stream()
                    .map(QueryWorker::getResult)
                    .flatMap(result -> ((HashMap<String, Long>) result).entrySet().stream())
                    .collect(HashMap<String, Long>::new,
                            (m, e) -> m.merge(e.getKey(), e.getValue(), Long::sum),
                            Map::putAll);
        }
        if ("processNodeFreq".equals(queryName)) {
            result = workers.stream()
                    .map(QueryWorker::getResult)
                    .flatMap(list -> ((List<Map.Entry<String, Integer>>) list).stream())
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .toList();
        } /* sort */
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
        synchronized (prerequisites) {
            while (hasPrerequisite()) {
                try {
                    prerequisites.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            String key1 = astID + queryName;
            prerequisites.put(key1, prerequisites.get(key1) - 1);
            if (queryName.equals("findSuperClasses")) {
                String key2 = key1 + args[0].toString();
                prerequisites.put(key2, prerequisites.get(key2) - 1);
            }
            runSerial();
            prerequisites.notifyAll();
        }
    }

    private boolean hasPrerequisite() {
        ArrayList<String> list = new ArrayList<>();
        switch (queryName) {
            case "haveSuperClass", "findAllMethods" -> list.add(astID + "findSuperClasses" + args[0]);
            case "findOverridingMethods" -> list.add(astID + "findSuperClasses");
            case "findClassesWithMain" -> {
                list.add(astID + "findSuperClasses");
                list.add(astID + "findAllMethods");
            }
            default -> {
            }
        }
        for (String key : list) {
            Integer val = prerequisites.get(key);
            if (val != null && val > 0)
                return true;
        }
        return false;
    }

    private static HashMap<String, Integer> prerequisites;

    public static void setPrerequisites(HashMap<String, Integer> prerequisites) {
        QueryWorker.prerequisites = prerequisites;
    }
}
