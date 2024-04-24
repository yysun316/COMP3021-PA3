package hk.ust.comp3021.parallel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import hk.ust.comp3021.RapidASTManagerEngine;
import hk.ust.comp3021.query.QueryOnClass;
import hk.ust.comp3021.utils.TestKind;

public class ParallelTest {

    @SuppressWarnings("unchecked")
    public void checkResults(List<Object> expecteds, List<Object> actuals, List<Object[]> commands) {
        for (int i = 0; i < expecteds.size(); i++) {
            Object expected = expecteds.get(i);
            Object actual = actuals.get(i);
            String queryName = (String) ((Object[]) commands.get(i))[2];

            if (queryName.equals("findClassesWithMain") || queryName.equals("findSuperClasses")) {
                assertEquals(expected, new HashSet<String>((List<String>) actual));
            } else {
                assertEquals(expected, actual);
            }
        }
    }

    @Tag(TestKind.PUBLIC)
    @Test
    public void testParallelLoadingPool() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();
        engine.processXMLParsingPool("resources/pythonxml/", List.of("18", "19", "20", "100"), 4);
        assertEquals(3, engine.getId2ASTModule().size());
    }


    @Tag(TestKind.PUBLIC)
    @Test
    public void testParallelLoadingDivide() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();
        engine.processXMLParsingDivide("resources/pythonxml/", List.of("18", "19", "20", "100"), 4);
        assertEquals(3, engine.getId2ASTModule().size());
    }

    
    @Tag(TestKind.PUBLIC)
    @Test
    public void testSerialExecution() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();
        engine.processXMLParsingPool("resources/pythonxml/", List.of("18", "19", "20"), 4);

        List<Object[]> commands = new ArrayList<>();
        List<Object> expectedResults = new ArrayList<>();
        commands.add(new Object[]{"1", "18", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"2", "19", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"3", "20", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"4", "18", "haveSuperClass", new Object[]{"B", "A"}});

        expectedResults.add(Set.of("B", "C", "D", "E", "F", "G", "H"));
        expectedResults.add(Set.of("C", "D", "F", "G", "H"));
        expectedResults.add(Set.of("B", "D"));
        expectedResults.add(true);

        engine.processCommands(commands, 0);
        List<Object> allResults = engine.getAllResults();
        checkResults(expectedResults, allResults, commands);
    }


    @Tag(TestKind.PUBLIC)
    @Test
    public void testParallelExecution() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();
        engine.processXMLParsingPool("resources/pythonxml/", List.of("18", "19", "1"), 4);

        List<Object[]> commands = new ArrayList<>();
        List<Object> expectedResults = new ArrayList<>();
        commands.add(new Object[]{"1", "18", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"2", "19", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"3", "0", "calculateOp2Nums", new Object[]{}});
        commands.add(new Object[]{"4", "18", "haveSuperClass", new Object[]{"B", "A"}});

        expectedResults.add(Set.of("B", "C", "D", "E", "F", "G", "H"));
        expectedResults.add(Set.of("C", "D", "F", "G", "H"));
        HashMap<String, Integer> m3 = new HashMap<>();
        m3.put("Eq", 3);
        expectedResults.add(m3);
        expectedResults.add(true);

        engine.processCommands(commands, 1);
        List<Object> allResults = engine.getAllResults();
        checkResults(expectedResults, allResults, commands);
    }

    @Tag(TestKind.PUBLIC)
    @Test
    public void testParallelExecutionWithOrder() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();
        engine.processXMLParsingPool("resources/pythonxml/", List.of("18", "19"), 4);

        List<Object[]> commands = new ArrayList<>();
        List<Object> expectedResults = new ArrayList<>();
        List<Integer> expectedCounts = List.of(2, 2, 0, 0,0);
        commands.add(new Object[] {"2", "18", "findSuperClasses", new Object[] {"B"}});
        commands.add(new Object[] {"3", "18", "haveSuperClass", new Object[] {"B", "A"}});
        commands.add(new Object[] {"3", "18", "haveSuperClass", new Object[] {"B", "H"}});

        expectedResults.add(Set.of("A"));
        expectedResults.add(true);
        expectedResults.add(false);

        QueryOnClass.clearCounts();
        engine.processCommands(commands, 2);
        List<Object> allResults = engine.getAllResults();
        List<Integer> allCounts = QueryOnClass.getCounts();
        checkResults(expectedResults, allResults, commands);
        assertEquals(expectedCounts, allCounts);
    }

    @Tag(TestKind.PUBLIC)
    @Test
    public void testInterleavedImportQuery() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();

        List<Object[]> commands = new ArrayList<>();
        List<Object> expectedResults = new ArrayList<>();
        commands.add(new Object[]{"1", "18", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"2", "19", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"3", "1", "calculateOp2Nums", new Object[]{}});
        commands.add(new Object[]{"4", "19", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"5", "18", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"6", "1", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"7", "18", "haveSuperClass", new Object[]{"B", "A"}});

        expectedResults.add(Set.of("B", "C", "D", "E", "F", "G", "H"));
        expectedResults.add(Set.of("C", "D", "F", "G", "H"));
        HashMap<String, Integer> m3 = new HashMap<>();
        m3.put("Eq", 3);
        expectedResults.add(m3);
        expectedResults.add(true);

        engine.processCommandsInterLeaved(commands);
        List<Object> allResults = engine.getAllResults();
        checkResults(expectedResults, allResults, commands);
    }

    @Tag(TestKind.PUBLIC)
    @Test
    public void testInterleavedImportQueryTwo() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();

        List<Object[]> commands = new ArrayList<>();
        List<Object> expectedResults = new ArrayList<>();
        commands.add(new Object[]{"1", "18", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"2", "19", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"3", "1", "calculateOp2Nums", new Object[]{}});
        commands.add(new Object[]{"4", "19", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"5", "18", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"6", "1", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"7", "18", "haveSuperClass", new Object[]{"B", "A"}});

        expectedResults.add(Set.of("B", "C", "D", "E", "F", "G", "H"));
        expectedResults.add(Set.of("C", "D", "F", "G", "H"));
        HashMap<String, Integer> m3 = new HashMap<>();
        m3.put("Eq", 3);
        expectedResults.add(m3);
        expectedResults.add(true);

        engine.processCommandsInterLeavedTwoThread(commands);
        List<Object> allResults = engine.getAllResults();
        checkResults(expectedResults, allResults, commands);
    }

    @Tag(TestKind.PUBLIC)
    @Test
    public void testInterleavedImportQueryBonus() {
        RapidASTManagerEngine engine = new RapidASTManagerEngine();

        List<Object[]> commands = new ArrayList<>();
        List<Object> expectedResults = new ArrayList<>();
        commands.add(new Object[]{"1", "18", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"2", "19", "findClassesWithMain", new Object[]{}});
        commands.add(new Object[]{"3", "1", "calculateOp2Nums", new Object[]{}});
        commands.add(new Object[]{"4", "19", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"5", "18", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"6", "1", "processXMLParsing", new Object[]{"resources/pythonxml/"}});
        commands.add(new Object[]{"7", "18", "haveSuperClass", new Object[]{"B", "A"}});

        expectedResults.add(Set.of("B", "C", "D", "E", "F", "G", "H"));
        expectedResults.add(Set.of("C", "D", "F", "G", "H"));
        HashMap<String, Integer> m3 = new HashMap<>();
        m3.put("Eq", 3);
        expectedResults.add(m3);
        expectedResults.add(true);

        engine.processCommandsInterLeavedFixedThread(commands, 3);
        List<Object> allResults = engine.getAllResults();
        checkResults(expectedResults, allResults, commands);
    }
}
