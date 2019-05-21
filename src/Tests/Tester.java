package Tests;

import main.ClojureTransformer;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.nio.file.FileSystems;

import static org.junit.Assert.assertEquals;


public class Tester {

    public void baseRunTest(String originalFile) {
        String transformedFile = originalFile.substring(0, originalFile.length() - 3) + "Ans.js";
        String srcPath = "\\src\\Tests\\";
        String simpleJSFile = new File("").getAbsolutePath() + srcPath + originalFile;
        String programResult = "C:\\Users\\Sergey\\Desktop\\JavaScriptChanger" + srcPath + transformedFile;
        ClojureTransformer.transformFile(simpleJSFile, programResult);
        String originalOutput = runFile(new File(simpleJSFile));
        String transformedOutput = runFile(new File(programResult));
        assertEquals("expected\n" + originalOutput + "\n finded\n" + transformedOutput, originalOutput, transformedOutput);
    }

    @Test
    public void sampleTest(){
        baseRunTest("Sample.js");
    }
    @Test
    public void checkSimpleJSFile() {
        baseRunTest("SimpleTest.js");
    }

    @Test
    public void checkOneNestedFunctionFile(){
        baseRunTest("OneNestedFunction.js");
    }

    @Test
    public void checkManyNestedFunctions(){
        baseRunTest("ManyNested.js");
    }

    @Test
    public void manySameLevelFunctions(){
        baseRunTest("ManySameLevelFunctions.js");
    }


    //put print block before each return
    //it lets to watch for calculating of programs
    //if calculating of 2 are equals it means(with good probability)
    //that both programs behavior are equals
    private File prepareFile(File file) {
        File out = new File(file.getParent(), file.getName().replace(".js", "Prepared.js"));
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(out))) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (line.contains("return ")) {

                        writer.println("print(" +
                                line.substring(line.indexOf("return ") + 7, line.length() - 1)
                                + ");\n");
                    }
                    writer.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return out;
    }

    private String runFile(File file) {
        File runedFile = prepareFile(file);
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        CharArrayWriter outputOfEvaluatedProgram = new CharArrayWriter();
        engine.getContext().setWriter(outputOfEvaluatedProgram);
        try {
            engine.eval(new FileReader(runedFile));
        } catch (ScriptException e) {
            System.err.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return new String(outputOfEvaluatedProgram.toCharArray());
    }
}
