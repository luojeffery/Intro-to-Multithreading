import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WordCounter implements Runnable {

    // The following are the ONLY variables we will modify for grading.
    // The rest of your code must run with no changes.
    public static final Path FOLDER_OF_TEXT_FILES  = Paths.get("");
    public static final Path WORD_COUNT_TABLE_FILE = Paths.get(""); // path to the output plain-text (.txt) file
    public static final int  NUMBER_OF_THREADS = 20;                // max. number of threads to spawn

    public static TreeMap<String, List<Integer>> treeMap = new TreeMap<>();
    public static List<String> wordlong= new ArrayList<>(); //ignores ties among the longest words
    public static File[] listFiles = WordCounter.parseFileList();

    private File file;
    private int index;

    public WordCounter() {}

    public WordCounter(File file, int index) {
        this.file = file;
        this.index = index;
    }

    public static void main(String... args) {
        try {
            if (!checkValidPath())
                throw new IllegalArgumentException("Invalid folder for text files.");
            ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            for (int i = 0; i < listFiles.length; i++) {
                WordCounter wc = new WordCounter(listFiles[i], i);
                executorService.execute(wc);
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            WordCounter wordCounter = new WordCounter();
            String output = wordCounter.toString();
            wordCounter.writetoFile(output);
        }
        catch(IllegalArgumentException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }
            String[] words = builder.toString().replaceAll("\\p{P}", "").toLowerCase().split("\\s+");
            synchronized (treeMap) {
                for (String w : words) {
                    if (!treeMap.containsKey(w)) {
                        treeMap.put(w, new ArrayList<>(Collections.nCopies(Objects.requireNonNull(FOLDER_OF_TEXT_FILES.toFile().list()).length, 0)));
                        int val = treeMap.get(w).get(index);
                        treeMap.get(w).set(index, ++val);
                    }
                    else {
                        int val = treeMap.get(w).get(index);
                        treeMap.get(w).set(index, ++val);
                    }

                }
                Arrays.sort(words, Comparator.comparingInt(String::length));
                wordlong.add(words[words.length - 1]); //ignores ties among the longest words
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String longestWord() { //ignores ties among the longest words
        wordlong = wordlong.stream()
                .sorted(Comparator.comparingInt(String::length))
                .collect(Collectors.toList());
        return wordlong.get(wordlong.size() - 1);
    }

    public String toString() {
        List<String> listNames = Arrays.stream(listFiles)
                .map(f -> f.getName().replace(".txt", ""))
                .collect(Collectors.toList());

        List<Integer> lengths = new ArrayList<>();
        listNames.forEach(n -> lengths.add(n.length()));

        String firstspace = "%" + -(longestWord().length() + 1) + "s"; //space needed for the first column
        StringBuilder output = new StringBuilder();

        //the next three lines is for the first row
        output.append(String.format(firstspace, ""));
        listNames.forEach(n -> output.append(String.format("%" + -n.length() + "s" + "%4s", n, "")));
        output.append("total\n");

        String[] keys = treeMap.keySet().toArray(new String[0]);

        for (String key : keys) {
            output.append(String.format(firstspace, key)); //adding in the word for the first column
            for (int j = 0; j < listNames.size(); j++) {
                output.append(String.format("%" + -(lengths.get(j)) + "s" + "%4s", treeMap.get(key).get(j), "")); //adding columns for every text file
            }
            output.append(treeMap.get(key).stream().mapToInt(Integer::intValue).sum()).append("\n"); //adds in the total and line break to continue to next word
        }
        return output.toString();
    }

    public void writetoFile(String output) {
        try {
            File file = WORD_COUNT_TABLE_FILE.toFile();
            if (!file.createNewFile()) {
                throw new IOException("IOException: File already exists.");
            }
            FileWriter writer = new FileWriter(file);
            writer.write(output);
            System.out.println("Wrote to file.");
            writer.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static File[] parseFileList() { //creates a more intuitive alphabetical order of the file list
        listFiles = FOLDER_OF_TEXT_FILES.toFile().listFiles();
        assert listFiles != null;
        TreeMap<String, File> filedict = new TreeMap<>();
        List<File> files = new ArrayList<>();
        Arrays.stream(listFiles).forEach(f -> {
            if (f.getName().endsWith(".txt") && !(f.getPath().equals(WORD_COUNT_TABLE_FILE.toString()))) {
                filedict.put(f.getName().replace(".txt", ""), f);
            }
        });
        Set<String> names = filedict.keySet(); //all the filenames so it's easier to sort functionally and without .txt
        List<String> sorted = names.stream()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
        sorted.forEach(f -> files.add(filedict.get(f)));
        return files.toArray(new File[0]);
    }

    public static boolean checkValidPath() {
        File folder = FOLDER_OF_TEXT_FILES.toFile();
        return folder.exists()
                && folder.isDirectory()
                && Objects.requireNonNull(folder.list()).length != 0;
    }
}