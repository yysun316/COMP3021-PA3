package hk.ust.comp3021;
import hk.ust.comp3021.query.*;
import hk.ust.comp3021.utils.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;


public class ASTManagerEngine {
    private final HashMap<String, ASTModule> id2ASTModules = new HashMap<>();
    public QueryOnNode queryOnNode = new QueryOnNode(id2ASTModules);


    public HashMap<String, ASTModule> getId2ASTModules() {
        return id2ASTModules;
    }


    public void userInterface() {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("ASTManager is running...");

        while (true) {
            System.out.println("----------------------------------------------------------------------");
            System.out.println("Please select the following operations with the corresponding numbers:");
            System.out.println("  0: Given AST ID, parse AST from XML files");
            // PA1 tasks, need to be rewritten with lambda expression
            System.out.println("  1: Print all functions with # arguments greater than user specified N");
            System.out.println("  2: Find the most commonly used operators in all ASTs");
            System.out.println("  3: Given AST ID, count the number of all node types");
            System.out.println("  4: Sort all functions based on # children nodes");

            // PA2 tasks code patterns for methods
            System.out.println("  5: Given func name, find all comparison expressions with \"==\"");
            System.out.println("  6: Find all functions using boolean parameter as an if-condition");
            System.out.println("  7: Given func name, find all unused parameters");
            System.out.println("  8: Given name of func B, find all functions being directly called by functions other than B");
            System.out.println("  9: Can func A directly or transitively call by method B");

            // PA2 tasks code patterns for class  10-14
            System.out.println("  10: Given name of class A, find all the super classes of it.");
            System.out.println("  11: Given the names of two classes, A and B, check whether A has super class B.");
            System.out.println("  12: Find all the overriding methods in all classes.");
            System.out.println("  13: Given the name of a class, find all the methods that it possesses.");
            System.out.println("  14: Find all the classes that possesses main function.");

            System.out.println("  16: Exit");
            System.out.println("----------------------------------------------------------------------");
            Scanner scan1 = new Scanner(System.in);
            if (scan1.hasNextInt()) {
                int i = scan1.nextInt();
                if (i < 0 || i > 18) {
                    System.out.println("You should enter 0~7.");
                    continue;
                }

                switch (i) {
                    case 0: {
                        userInterfaceParseXML();
                        break;
                    }
                    case 1: {
                        userInterfaceParamNum();
                        break;
                    }
                    case 2: {
                        userInterfaceCommonOp();
                        break;
                    }
                    case 3: {
                        userInterfaceCountNum();
                        break;
                    }
                    case 4: {
                        userInterfaceSortByChild();
                        break;
                    }
                    case 5: {
                        userInterfaceFindEqual();
                        break;
                    }
                    case 6: {
                        userInterfaceFindBoolAsIf();
                        break;
                    }
                    case 7: {
                        userInterfaceFindUnused();
                        break;
                    }
                    case 8: {
                        userInterfaceCallOtherB();
                        break;
                    }
                    case 9: {
                        userInterfaceDirectOrTransCall();
                        break;
                    }
                    case 10: {
                        userInterfaceFindSuperClasses();
                        break;
                    }
                    case 11: {
                        userInterfaceHaveSuperClass();
                        break;
                    }
                    case 12: {
                        userInterfaceFindOverridingMethods();
                        break;
                    }
                    case 13: {
                        userInterfaceFindAllMethods();
                        break;
                    }
                    case 14: {
                        userInterfaceFindClassesWithMain();
                        break;
                    }
                    default: {

                    }
                }
                if (i == 16) {
                    break;
                }
            } else {
                System.out.println("You should enter integer 0~6.");
            }
        }
    }


    public int countXMLFiles(String dirPath) {
        int count = 0;
        File directory = new File(dirPath);
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        count += countXMLFiles(file.getAbsolutePath());
                    } else if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                        count++;
                    }
                }
            }
        }

        return count;
    }


    /*
     * Task 0: Given AST ID, parse AST from XML files
     */

    public void processXMLParsing(String xmlDirPath, String xmlID) {
        ASTParser parser = new ASTParser(Paths.get(xmlDirPath).resolve("python_" + xmlID + ".xml").toString());
        parser.parse();
        if (!parser.isErr()) {
            this.id2ASTModules.put(xmlID, parser.getASTModule());
            System.out.println("AST " + xmlID + " Succeed! The XML file is loaded!");
        } else {
            System.out.println("AST " + xmlID + " Failed! Please check your implementation!");
        }
    }


    public void userInterfaceParseXML() {
        System.out.println("Please provide the XML directory to load");
        Scanner scan1 = new Scanner(System.in);
        if (scan1.hasNextLine()) {
            String xmlFileDir = scan1.nextLine();
            int xmlCount = countXMLFiles(xmlFileDir);
            System.out.println("Please specify the XML file ID to parse (0~" + xmlCount + ") or -1 for all:");
            if (scan1.hasNextLine()) {
                String xmlID = scan1.nextLine();
                if (!xmlID.equals("-1")) {
                    processXMLParsing(xmlFileDir, xmlID);
                } else {
                    File directory = new File(xmlFileDir);
                    if (directory.isDirectory()) {
                        File[] files = directory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
                                    String str = file.getName().toLowerCase();
                                    int startIndex = str.indexOf('_') + 1;
                                    int endIndex = str.indexOf(".xml");

                                    if (endIndex > startIndex) {
                                        processXMLParsing(xmlFileDir,
                                                str.substring(startIndex, endIndex));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Task 1: Print all functions with # arguments greater than user specified N
     */
    public void userInterfaceParamNum() {
        System.out.println("Please indicate the value of N (recommended range 0~5):");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String paramN = scan2.nextLine();
            try {
                int number = Integer.parseInt(paramN);
                System.out.println("Parsed number: " + number);
                List<String> result = queryOnNode.findFuncWithArgGtN.apply(number);
                result.forEach(System.out::println);
            } catch (NumberFormatException e) {
                System.out.println("Error! Invalid number format");
            }
        }
    }

    /*
     * Task 2: Find the most commonly used operators in all ASTs
     */
    public void userInterfaceCommonOp() {
        HashMap<String, Integer> op2Num = queryOnNode.calculateOp2Nums.get();
        Map.Entry<String, Integer> maxEntry = Collections.max(op2Num.entrySet(),
                Map.Entry.comparingByValue());
        System.out.println("Most common operator is " + maxEntry.getKey() + " with frequency " + maxEntry.getValue());
    }


    /*
     * Task 3: Given AST ID, count the number of all node types
     */
    public void userInterfaceCountNum() {
        System.out.println("Please specify the AST ID to count Node (" + id2ASTModules.keySet() + ") or -1 for all:");
        Scanner scan1 = new Scanner(System.in);
        if (scan1.hasNextLine()) {
            String astID = scan1.nextLine();
            queryOnNode.calculateNode2Nums.apply(astID).forEach((key, value) ->
                    System.out.println(astID + key + " node with frequency " + value));
        }
    }

    /*
     * Task 4: Sort all functions based on # children nodes
     */
    public void userInterfaceSortByChild() {
        queryOnNode.processNodeFreq
                .get()
                .forEach(entry -> System.out.println("Func " + entry.getKey() + " has complexity " + entry.getValue()));
    }


    /*
     * Task 5: Given func name, find all comparison expressions with \"==\"
     */
    public void userInterfaceFindEqual() {
        String queryID = this.parseQueryASTID();
        QueryOnMethod queryOnMethod = new QueryOnMethod(id2ASTModules.get(queryID));
        System.out.println("Please indicate the func name to be queried (Format: funcName, e.g., foo)");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String funcName = scan2.nextLine();
            queryOnMethod.findEqualCompareInFunc.apply(funcName).forEach(System.out::println);
        }

    }


    /*
     * Task 6: Find all functions using boolean parameter as an if-condition
     */
    public void userInterfaceFindBoolAsIf() {
        String queryID = this.parseQueryASTID();
        QueryOnMethod queryOnMethod = new QueryOnMethod(id2ASTModules.get(queryID));
        queryOnMethod.findFuncWithBoolParam.get().forEach(System.out::println);
    }

    /*
     * Task 7: Given func name, find all unused parameters
     */
    public void userInterfaceFindUnused() {
        String queryID = this.parseQueryASTID();
        QueryOnMethod queryOnMethod = new QueryOnMethod(id2ASTModules.get(queryID));
        System.out.println("Please indicate the func name to be queried (Format: funcName, e.g., foo)");
        Scanner scan2 = new Scanner(System.in);

        if (scan2.hasNextLine()) {
            String funcName = scan2.nextLine();
            queryOnMethod.findUnusedParamInFunc.apply(funcName).forEach(System.out::println);
        }
    }

    /*
     * Task 8: Given name of func B, find all functions being directly called by functions other than B
     */
    public void userInterfaceCallOtherB() {
        String queryID = this.parseQueryASTID();
        QueryOnMethod queryOnMethod = new QueryOnMethod(id2ASTModules.get(queryID));
        System.out.println("Please indicate the name of function B (Format: funcName, e.g., foo)");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String funcName = scan2.nextLine();
            queryOnMethod.findDirectCalledOtherB.apply(funcName).forEach(System.out::println);
        }
    }

    /*
     * Task 9: Given name of func B, find all functions being directly called by functions other than B
     */
    public void userInterfaceDirectOrTransCall() {
        String queryID = this.parseQueryASTID();
        QueryOnMethod queryOnMethod = new QueryOnMethod(id2ASTModules.get(queryID));
        System.out.println("Please indicate the name of function A and B (Format: funcName, e.g., foo)");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String funcNameA = scan2.nextLine();
            if (scan2.hasNextLine()) {
                String funcNameB = scan2.nextLine();
                System.out.println("Answer is " + queryOnMethod.answerIfACalledB.test(funcNameA, funcNameB));
            }
        }
    }

    /*
     * Task 10: Given name of class A, find all the super classes of it.
     */
    public void userInterfaceFindSuperClasses() {
        String queryID = this.parseQueryASTID();
        QueryOnClass queryOnClass = new QueryOnClass(id2ASTModules.get(queryID));
        System.out.println("Please indicate the name of the class");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String className = scan2.nextLine();
            System.out.println("Answer is " + queryOnClass.findSuperClasses.apply(className));
        }
    }

    /*
     * Task 11: Given the names of two classes, A and B, check whether A has super class B.
     */
    public void userInterfaceHaveSuperClass() {
        String queryID = this.parseQueryASTID();
        QueryOnClass queryOnClass = new QueryOnClass(id2ASTModules.get(queryID));
        System.out.println("Please indicate the two classes");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String classA = scan2.nextLine();
            if(scan2.hasNextLine()) {
                String classB = scan2.nextLine();
                System.out.println("Answer is " + queryOnClass.haveSuperClass.apply(classA, classB));
            }

        }
    }


    /*
     * Task 12: Find all the overriding methods in all classes.
     */
    public void userInterfaceFindOverridingMethods() {
        String queryID = this.parseQueryASTID();
        QueryOnClass queryOnClass = new QueryOnClass(id2ASTModules.get(queryID));
        Scanner scan2 = new Scanner(System.in);
        System.out.println("Answer is " + queryOnClass.findOverridingMethods.get());
    }

    /*
     * Task 13: Given the name of a class, find all the methods that it possesses.
     */
    public void userInterfaceFindAllMethods() {
        String queryID = this.parseQueryASTID();
        QueryOnClass queryOnClass = new QueryOnClass(id2ASTModules.get(queryID));
        System.out.println("Please indicate the the class");
        Scanner scan2 = new Scanner(System.in);
        if (scan2.hasNextLine()) {
            String className = scan2.nextLine();
            System.out.println("Answer is " + queryOnClass.findAllMethods.apply(className));
        }
    }

    /*
     * Task 14: Find all the classes that possesses main function.
     */
    public void userInterfaceFindClassesWithMain() {
        String queryID = this.parseQueryASTID();
        QueryOnClass queryOnClass = new QueryOnClass(id2ASTModules.get(queryID));
        Scanner scan2 = new Scanner(System.in);
        System.out.println("Answer is " + queryOnClass.findClassesWithMain.get());

    }


    private String parseQueryASTID() {
        System.out.println("Please specify the AST ID to query (" + id2ASTModules.keySet() + ")");
        Scanner scan0 = new Scanner(System.in);
        while (scan0.hasNextLine()) {
            String tobeQueriedID = scan0.nextLine();
            if (id2ASTModules.containsKey(tobeQueriedID)) {
                return tobeQueriedID;
            }
            System.out.println("Invalid! Please specify the AST ID to query (" + id2ASTModules.keySet() + ")");
        }
        return "";
    }
}

