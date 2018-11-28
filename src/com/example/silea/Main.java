package com.example.silea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void main (String args[]) {
        ////////////////
        // Parameters //
        ////////////////
        String train_test_file_path = ""; // train or test file path
        ArrayList<Integer> quantization_levels = new ArrayList<>(); // holds quantization levels, it is either empty, or size=1 or size=attributesize
        int no_of_conditions = 1; // beginning no. of conditions. user can change
        int mode = 0; // 0 default, 1 train, 2 test
        String model_filename = "";
        boolean check_partial_match = true;
        boolean verbose = false;

        //////////////////////////////////
        // Get attributes from the user //
        //////////////////////////////////
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-i") || args[i].equals("--input")) {
                train_test_file_path = args[i+1];
                i++;
            }
            else if (args[i].equals("-c") || args[i].equals("--condition")) {
                no_of_conditions = Integer.parseInt(args[i+1]);
                i++;
            }
            else if (args[i].equals("-prm") || args[i].equals("--partial-rule-match")) {
                if (Integer.parseInt(args[i+1]) == 0)
                    check_partial_match = false;
                else
                    check_partial_match = true;

                i++;
            }
            else if (args[i].equals("-m") || args[i].equals("--model-filename")) {
                model_filename = args[i+1];
                i++;
            }
            else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                verbose = true;
            }
            else if (args[i].equals("-q") || args[i].equals("--quantization")) {
                String[] temp = args[i+1].split(",");

                for (int j = 0; j < temp.length; j++)
                    quantization_levels.add(Integer.parseInt(temp[j]));

                i++;
            }
            else if (args[i].equals("-t") || args[i].equals("--train")) {
                mode = 1;
            }
            else if (args[i].equals("-T") || args[i].equals("--test")) {
                mode = 2;
            }
            else {
                System.out.println(ANSI_RED + "Unknown parameter '" + args[i] + "' " + ANSI_GREEN + "Type -h to see the help menu." + ANSI_RESET);
                System.exit(0);
            }
        }

        if (quantization_levels.size() == 0)
            quantization_levels.add(0);

        if (mode == 1)
            train(train_test_file_path, quantization_levels, no_of_conditions, model_filename, verbose); // returns a model
        else if (mode == 2)
            test(model_filename, train_test_file_path, check_partial_match, verbose);
        else {
            System.out.println("ERROR! Mode not provided!");
            System.exit(0);
        }
    }

    public static void read_model_file(String model_filename, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges, ArrayList<ArrayList<RuleObject2>> rules) {
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(model_filename));
            String inputFileCurrentLine;

            // read attribute types
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0) {
                    if (inputFileCurrentLine.equals("***"))
                        break;
                    else
                        quantization_levels.add(Integer.parseInt(inputFileCurrentLine));
                }
            }

            // read ranges
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0) {
                    if (inputFileCurrentLine.equals("***"))
                        break;
                    else {
                        ArrayList<Double> temp = new ArrayList<>();

                        if (inputFileCurrentLine.split(":").length > 1) {
                            String[] temp_array = inputFileCurrentLine.split(":")[1].split(",");

                            for (int i = 0; i < temp_array.length; i++)
                                temp.add(Double.parseDouble(temp_array[i]));
                        }

                        ranges.add(temp);
                    }
                }
            }

            // read rules
            ArrayList<RuleObject2> rule = null;
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0) {
                    if (!inputFileCurrentLine.equals("+++")) {
                        if (inputFileCurrentLine.split(":")[0].equals("condition"))
                            rule = new ArrayList<>();
                        else {
                            RuleObject2 temp2 = new RuleObject2();
                            temp2.occurence = Integer.parseInt(inputFileCurrentLine.split(";")[0]);
                            temp2.rule = inputFileCurrentLine.split(";")[1].split(",");
                            rule.add(temp2);
                        }
                    }
                    else
                        rules.add(rule);
                }
            }

            inputFile.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public static ArrayList<ArrayList<Integer>> get_nCr(int n, int r) {
        ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();

        int[] res = new int[r];
        for (int i = 0; i < res.length; i++)
            res[i] = i + 1;

        boolean done = false;

        while (!done) {
            ArrayList<Integer> temp = new ArrayList<>();
            for (int i = 0; i < res.length; i++)
                temp.add(res[i]);
            combinations.add(temp);

            done = getNext(res, n, r);
        }

        return combinations;
    }

    public static boolean getNext(int[] num, int n, int r) {
        int target = r - 1;
        num[target]++;
        if (num[target] > ((n - (r - target)) + 1)) {
            // Carry the One
            while (num[target] > ((n - (r - target)))) {
                target--;
                if (target < 0) {
                    break;
                }
            }
            if (target < 0) {
                return true;
            }
            num[target]++;
            for (int i = target + 1; i < num.length; i++) {
                num[i] = num[i - 1] + 1;
            }
        }
        return false;
    }

    public static void test(String model_filename, String train_test_file_path, boolean check_partial_match, boolean verbose) {
        ArrayList<Integer> quantization_levels = new ArrayList<>();
        ArrayList<ArrayList<Double>> ranges = new ArrayList<>();
        ArrayList<Example> examples = new ArrayList<>();
        ArrayList<ArrayList<RuleObject2>> rules = new ArrayList<>();
        int no_of_attributes = 0;

        read_model_file(model_filename, quantization_levels, ranges, rules);

        no_of_attributes = quantization_levels.size();

        read_train_test_file(train_test_file_path, examples, quantization_levels);

        if (verbose)
            print_examples(examples);

        System.out.println("Quantizing...");
        // Set ranges
        System.out.println("Setting ranges...");
        set_ranges(examples, quantization_levels, ranges, no_of_attributes);

        if (verbose) {
            System.out.println();
            print_examples(examples);
        }

        System.out.println("Quantization levels:");
        System.out.println(Arrays.toString(quantization_levels.toArray()));
        System.out.println();

        System.out.println("Ranges:");
        for (int i = 0; i < ranges.size(); i++) {
            ArrayList<Double> range = ranges.get(i);
            System.out.print(i + " -> ");
            for (int j = 0; j < range.size(); j++) {
                System.out.print(range.get(j));
                if (j < range.size()-1)
                    System.out.print(",");
            }
            System.out.println();
        }
        System.out.println();

        for (int i = 0; i < rules.size(); i++) { // for each condition
            if (rules.get(i).size() > 0) { // if the condition has rules in it
                System.out.println("Condition: " + (i+1));
                for (int j = 0; j < rules.get(i).size(); j++) { // for each rule in the condition
                    System.out.println(Arrays.toString(rules.get(i).get(j).rule) + " (" + rules.get(i).get(j).occurence + ")");
                }
                System.out.println();
            }
        }

        // add the position of examples which are not classified yet in a map
        Map<Integer,Integer> examples_unclassified = new HashMap<>();
        for (int i = 0; i < examples.size(); i++)
            examples_unclassified.put(i, -1);

        // classify (full match)
        int no_of_correctly_classified = 0;
        int no_of_incorrectly_classified = 0;

        for (int i = 0; i < examples.size(); i++) { // for each example
            boolean example_classified = false;
            String[] example_tokens = examples.get(i).tokens;

            if (verbose)
                System.out.println("example: " + Arrays.toString(example_tokens));

            for (int j = rules.size()-1; j >= 0; j--) { // for each condition (starting with the largest)
                if (verbose)
                    System.out.println("    condition:" + (j+1));
                ArrayList<RuleObject2> condition_rules = rules.get(j);

                for (int k = 0; k < condition_rules.size(); k++) { // for each rule
                    int no_of_match = 0;
                    String[] rule_tokens = condition_rules.get(k).rule;

                    if (verbose)
                        System.out.println("        rule: " + Arrays.toString(rule_tokens));

                    for (int l = 0; l < (rule_tokens.length-1); l++) { // for each token except for the class
                        int position = Integer.parseInt(rule_tokens[l].split(":")[0]);
                        if (example_tokens[position].equals(rule_tokens[l].split(":")[1]))
                            no_of_match++;
                    }

                    if (verbose)
                        System.out.println("            no_of_match: " + no_of_match + "/" + (rule_tokens.length - 1));

                    if (no_of_match == (rule_tokens.length - 1)) { // if features match
                        examples_unclassified.remove(i);
                        example_classified = true;

                        if (rule_tokens[rule_tokens.length-1].equals(example_tokens[example_tokens.length-1])) { // if classes match
                            no_of_correctly_classified++;
                            if (verbose)
                                System.out.println("                example classified correctly!");
                        }
                        else {
                            no_of_incorrectly_classified++;
                            if (verbose)
                                System.out.println("                example classified incorrectly!");
                        }
                    }

                    if (example_classified)
                        break;
                }

                if (example_classified)
                    break;
            }

            if (verbose)
                System.out.println();
        }

        System.out.println("Full match");
        System.out.println("Correctly classified: " + no_of_correctly_classified + "/" + examples.size() + " (" + (((double) no_of_correctly_classified / (double) examples.size()) * 100) +"%)");
        System.out.println("Incorrectly classified: " + no_of_incorrectly_classified + "/" + examples.size() + " (" + (((double) no_of_incorrectly_classified / (double) examples.size()) * 100) +"%)");
        System.out.println("Unclassified: " + examples_unclassified.size() + "/" + examples.size() + " (" + (((double) examples_unclassified.size() / (double) examples.size()) * 100) +"%)");

        // partial match
        if (examples_unclassified.size() > 0 && check_partial_match) {
            for (int i = 0; i < examples.size(); i++) { // for each example
                if (examples_unclassified.containsKey(i)) { // if example is not classified
                    boolean example_classified = false;
                    String[] example_tokens = examples.get(i).tokens;

                    if (verbose)
                        System.out.println("example: " + Arrays.toString(example_tokens));

                    for (int error = 1; error < example_tokens.length-1; error++) { // the number of error features allowed
                        if (verbose)
                            System.out.println("ERROR: " + error);

                        for (int j = rules.size()-1; j >= 0; j--) { // for each condition (starting with the largest)
                            if (j > error) {
                                if (verbose)
                                    System.out.println("    condition:" + (j+1));
                                ArrayList<RuleObject2> condition_rules = rules.get(j);

                                for (int k = 0; k < condition_rules.size(); k++) { // for each rule
                                    int no_of_match = 0;
                                    String[] rule_tokens = condition_rules.get(k).rule;

                                    if (verbose)
                                        System.out.println("        rule: " + Arrays.toString(rule_tokens));

                                    for (int l = 0; l < (rule_tokens.length-1); l++) { // for each token except for the class
                                        int position = Integer.parseInt(rule_tokens[l].split(":")[0]);
                                        if (example_tokens[position].equals(rule_tokens[l].split(":")[1]))
                                            no_of_match++;
                                    }

                                    if (verbose)
                                        System.out.println("            no_of_match: " + no_of_match + "/" + (rule_tokens.length - 1));

                                    if ((no_of_match + error) >= (rule_tokens.length - 1)) { // if features match
                                        examples_unclassified.remove(i);
                                        example_classified = true;

                                        if (rule_tokens[rule_tokens.length-1].equals(example_tokens[example_tokens.length-1])) { // if classes match
                                            no_of_correctly_classified++;
                                            if (verbose)
                                                System.out.println("                example classified correctly!");
                                        }
                                        else {
                                            no_of_incorrectly_classified++;
                                            if (verbose)
                                                System.out.println("                example classified incorrectly!");
                                        }
                                    }

                                    if (example_classified)
                                        break;
                                }

                                if (example_classified)
                                    break;
                            }
                        }

                        if (verbose)
                            System.out.println();

                        if (example_classified)
                            break;
                    }
                }
            }

            System.out.println();
            System.out.println("Partial match");
            System.out.println("Correctly classified: " + no_of_correctly_classified + "/" + examples.size() + " (" + (((double) no_of_correctly_classified / (double) examples.size()) * 100) +"%)");
            System.out.println("Incorrectly classified: " + no_of_incorrectly_classified + "/" + examples.size() + " (" + (((double) no_of_incorrectly_classified / (double) examples.size()) * 100) +"%)");
            System.out.println("Unclassified: " + examples_unclassified.size() + "/" + examples.size() + " (" + (((double) examples_unclassified.size() / (double) examples.size()) * 100) +"%)");
        }
    }

    public static void read_train_test_file(String train_test_file_path, ArrayList<Example> examples, ArrayList<Integer> quantization_levels) {
        ArrayList<String> attribute_types = new ArrayList<>();

        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(train_test_file_path));
            String inputFileCurrentLine;

            // read attribute types
            while (((inputFileCurrentLine = inputFile.readLine()) != null)) {
                if (inputFileCurrentLine.trim().length() != 0 && inputFileCurrentLine.charAt(0) == '@') {
                    String tokens[] = inputFileCurrentLine.split("\\s+");

                    if (tokens[0].toLowerCase().equals("@data"))
                        break;

                    if (tokens[0].toLowerCase().equals("@attribute")) {
                        // set to numeric or string
                        String type = tokens[tokens.length - 1].toLowerCase();
                        if (type.equals("numeric") || type.equals("real"))
                            attribute_types.add("numeric");
                        else
                            attribute_types.add("string");
                    }
                }
            }

            // remove the class from attributes (which is the last entry)
            attribute_types.remove(attribute_types.size()-1);

            // regenerate missing quantization levels
            if (quantization_levels.size() == 1) {
                int x = quantization_levels.get(0);
                for (int i = 0; i < (attribute_types.size()-1); i++)
                    quantization_levels.add(x);
            }

            // Check if correct number of quantization levels are provided
            if (quantization_levels.size() != attribute_types.size()) {
                System.out.println("Number of quantization levels is incorrect!");
                System.exit(0);
            }

            // if user cancelled quantizations for some of the parameters
            for (int i = 0; i < quantization_levels.size(); i++)
                if (quantization_levels.get(i) > 0 && attribute_types.get(i).equals("string"))
                    quantization_levels.set(i, 0);

            // read examples
            // For each line in the input file
            while ((inputFileCurrentLine = inputFile.readLine()) != null) {
                if (inputFileCurrentLine.trim().length() != 0 && inputFileCurrentLine.charAt(0) != '@') {
                    Example temp = new Example();
                    temp.tokens = inputFileCurrentLine.split(",");
                    examples.add(temp);
                }
            }

            inputFile.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public static void train(String train_test_file_path, ArrayList<Integer> quantization_levels, int no_of_conditions, String model_filename, boolean verbose) {
        ArrayList<ArrayList<Double>> ranges = new ArrayList<>();
        ArrayList<Example> examples = new ArrayList<>();
        ArrayList<Integer> entropy_positions = new ArrayList<>();
        int no_of_attributes = 0;

        // Read train/test file contents to ArrayList
        System.out.println("Reading examples...");
        read_train_test_file(train_test_file_path, examples, quantization_levels);

        System.out.println();

        no_of_attributes = quantization_levels.size();

        // Print example size
        System.out.println("No of examples read: " + examples.size());
        System.out.println("No of attributes: " + no_of_attributes);
        System.out.println();

        System.out.println("Quantizing...");
        // Find ranges
        System.out.println("Calculating ranges...");
        find_ranges(no_of_attributes, examples, quantization_levels, ranges);

        // Set ranges
        System.out.println("Setting ranges...");
        set_ranges(examples, quantization_levels, ranges, no_of_attributes);

        // Find hash (the reason for doing this here is after quantization, hash might change)
        for (int i = 0; i < examples.size(); i++)
            examples.get(i).hash = String.join(",", Arrays.copyOfRange(examples.get(i).tokens, 0, examples.get(i).tokens.length-1)).hashCode();

        // Sort examples
        System.out.println("Sorting examples...");
        Collections.sort(examples, new Comparator<Example>() {
            @Override public int compare(Example bo1, Example bo2) {
                if (bo1.hash > bo2.hash)
                    return 1;
                else if (bo1.hash < bo2.hash)
                    return -1;
                else
                    return 0;
            }
        });
        System.out.println();

        if (verbose) {
            System.out.println("After sorting examples:");
            print_examples(examples);
        }

        // Find examples with different classes
        remove_examples_with_different_classes(examples);

        if (verbose) {
            System.out.println("After removing repetitions:");
            print_examples(examples);
        }

        // Calculate entropies
        System.out.println("Calculating entropies...");
        System.out.println();
        entropy_positions = calculate_entropies(no_of_attributes, examples, verbose);

        System.out.println("Feature order by entropy:");
        System.out.println(Arrays.toString(entropy_positions.toArray()));
        System.out.println();

        System.out.println("Attribute types:");
        System.out.print("[");
        for (int i = 0; i < quantization_levels.size(); i++) {
            if (quantization_levels.get(i) == 0)
                System.out.print("string");
            else
                System.out.print("numeric");

            if (i < quantization_levels.size() - 1)
                System.out.print(", ");
        }
        System.out.print("]");
        System.out.println();
        System.out.println();

        System.out.println("Quantization levels:");
        System.out.println(Arrays.toString(quantization_levels.toArray()));
        System.out.println();

        System.out.println("Ranges:");
        for (int i = 0; i < ranges.size(); i++) {
            System.out.print(i + " -> ");
            System.out.println(Arrays.toString(ranges.get(i).toArray()));
        }
        System.out.println();

        // Print example size
        System.out.println("No of examples after pre-processing: " + examples.size());
        System.out.println();

        if (verbose) {
            System.out.println("Examples to be processed:");
            print_examples(examples);
        }

        // Extract rules
        System.out.println("Extracting rules...");
        System.out.println();
        ArrayList<ArrayList<RuleObject2>> rules = new ArrayList<>();

        // add the position of examples which are not classified yet in a map
        Map<Integer,Integer> examples_unclassified = new HashMap<>();
        for (int i = 0; i < examples.size(); i++)
            examples_unclassified.put(i, -1);

        if (verbose) {
            System.out.println("No. of examples left to be classified: " + examples_unclassified.size());
            System.out.println();
        }

        while ((examples_unclassified.size() > 0) && (no_of_conditions <= no_of_attributes)) {
            Map<String, RuleObject> potential_list = new HashMap<>();
            Map<String, Integer> black_list = new HashMap<>(); // integer is not needed here

            System.out.println("Condition: "  + no_of_conditions);

            for (int i = 0; i < examples.size(); i++) { // for each example
                if (verbose) {
                    System.out.println("    Example:");
                    System.out.println("    " + Arrays.toString(examples.get(i).tokens));
                }

                String[] rule = new String[no_of_conditions+1]; // +1 is for class

                boolean skip = false;

                // add pre-rule
                for (int j = 0; j < (no_of_conditions - 1); j++) {
                    if (examples.get(i).tokens[entropy_positions.get(j)].equals("?")) {
                        skip = true;
                        break;
                    }

                    rule[j] = entropy_positions.get(j) + ":" + examples.get(i).tokens[entropy_positions.get(j)];
                }

                if (!skip) {
                    // add class
                    rule[no_of_conditions] = examples.get(i).tokens[no_of_attributes];

                    // add the remaining feature one at a time
                    if (verbose)
                        System.out.println("        Rules:");
                    for (int j = (no_of_conditions - 1); j < no_of_attributes; j++) {
                        if (!examples.get(i).tokens[entropy_positions.get(j)].equals("?")) { // skip if the feature being added is null
                            rule[no_of_conditions-1] = entropy_positions.get(j) + ":" + examples.get(i).tokens[entropy_positions.get(j)];
                            if (verbose)
                                System.out.println("        " + Arrays.toString(rule));

                            String[] rule_features = Arrays.copyOfRange(rule, 0, rule.length-1); // get the rule without class

                            if (verbose)
                                System.out.println("            Rule feature is: " + Arrays.toString(rule_features));

                            if (!black_list.containsKey(String.join(",", rule_features))) { // if the rule is not blacklisted
                                if (verbose)
                                    System.out.println("            Rule is not blacklisted!");

                                if (potential_list.containsKey(String.join(",", rule_features))) { // if the rule is in potentiallist
                                    if (verbose)
                                        System.out.println("            Rule is in potentiallist!");

                                    if (potential_list.get(String.join(",", rule_features)).rule_class.equals(examples.get(i).tokens[no_of_attributes])) { // if the class is the same
                                        potential_list.get(String.join(",", rule_features)).occurence++;
                                        potential_list.get(String.join(",", rule_features)).classifies.put(i, -1);
                                        if (verbose)
                                            System.out.println("            Rule exists in potentiallist with same class, incrementing occurrence!");
                                    }
                                    else { // if the class is different, add to blacklist
                                        black_list.put(String.join(",", rule_features), null);
                                        potential_list.remove(String.join(",", rule_features));
                                        if (verbose)
                                            System.out.println("            Rule exists in potentiallist with different class, removing!");
                                    }
                                }
                                else { // add the rule
                                    RuleObject temp = new RuleObject();
                                    temp.occurence = 1;
                                    temp.classifies.put(i, -1);
                                    temp.rule_class = examples.get(i).tokens[no_of_attributes];

                                    potential_list.put(String.join(",", rule_features), temp);

                                    if (verbose)
                                        System.out.println("            Rule is added to the potentiallist!");
                                }
                            }
                            else {
                                if (verbose)
                                    System.out.println("            Rule is blacklisted!");
                            }
                        }
                    }

                    if (verbose)
                        System.out.println();
                }
            }

            ArrayList<RuleObject2> current_rules = new ArrayList<>();
            for (String entry : potential_list.keySet()) {
                RuleObject2 temp = new RuleObject2();
                temp.occurence = potential_list.get(entry).occurence;
                for (int entry2 : potential_list.get(entry).classifies.keySet())
                    temp.classifies.put(entry2, -1);
                temp.rule = (entry + "," + potential_list.get(entry).rule_class).split(",");
                current_rules.add(temp);
            }

            // sort rules
            Collections.sort(current_rules, new Comparator<RuleObject2>() {
                @Override public int compare(RuleObject2 bo1, RuleObject2 bo2) {
                    if (bo1.occurence > bo2.occurence)
                        return -1;
                    else if (bo1.occurence < bo2.occurence)
                        return 1;
                    else
                        return 0;
                }
            });

            if (verbose) {
                System.out.println("Rules before:");
                for (int j = 0; j < current_rules.size(); j++)
                    System.out.println(Arrays.toString(current_rules.get(j).rule) + " (" + current_rules.get(j).occurence + ")");
                System.out.println();
            }

            int examples_unclassified_size_before = examples_unclassified.size();

            //classify_examples(examples_unclassified, current_rules);
            for (int i = 0; i < current_rules.size(); i++) { // for each rule
                int temp_size = examples_unclassified.size(); // find the current no of unclassified examples

                for (int entry : current_rules.get(i).classifies.keySet()) // remove each example that this rule classifies
                    examples_unclassified.remove(entry); // remove the examples that this rule can classify

                if ((temp_size - examples_unclassified.size()) == 0) { // if no examples were classified
                    current_rules.remove(i); // remove the rule
                    i--;
                }
            }

            if (verbose) {
                if (current_rules.size() > 0) {
                    System.out.println("    Rules(" + current_rules.size() + "):");
                    for (int j = 0; j < current_rules.size(); j++)
                        System.out.println("        " + Arrays.toString(current_rules.get(j).rule) + " (" + current_rules.get(j).occurence + ")");
                    System.out.println();
                }
                System.out.println("    No. of examples classified by extracted rules: " + (examples_unclassified_size_before - examples_unclassified.size()));
                System.out.println("    No. of examples left to be classified: " + examples_unclassified.size());
                System.out.println();
            }

            rules.add(current_rules);

            no_of_conditions++;
        }

        System.out.println();
        System.out.println("No. of classified examples: " + (examples.size() - examples_unclassified.size()));
        System.out.println("No. of unclassified examples: " + examples_unclassified.size());
        System.out.println("Classification rate: " + (((double)(examples.size() - examples_unclassified.size()) / (double)examples.size()) * 100) + "%");
        System.out.println("Misclassification rate: " + (((double)(examples_unclassified.size()) / (double)examples.size()) * 100) + "%");
        System.out.println();
        System.out.println("Rules extracted (model style):");
        System.out.println();

        for (int i = 0; i < rules.size(); i++) { // for each condition
            if (rules.get(i).size() > 0) { // if the condition has rules in it
                System.out.println("Condition: " + (i+1));
                for (int j = 0; j < rules.get(i).size(); j++) { // for each rule in the condition
                    System.out.println(Arrays.toString(rules.get(i).get(j).rule) + " (" + rules.get(i).get(j).occurence + ")");

                    if (verbose) {
                        System.out.println("    Classifies:");
                        System.out.print("    [");
                        for (int entry : rules.get(i).get(j).classifies.keySet())
                            System.out.print(entry + ", ");
                        System.out.println("]");
                    }
                }
                System.out.println();
            }
        }

/*
		if (verbose) {
			System.out.println();
			System.out.println("Rules extracted (human-readable):");
			System.out.println();
			print_rules_human_readable(rules, attribute_types, ranges);
		}
*/

        if (!model_filename.equals(""))
            write_model_to_file(model_filename, quantization_levels, ranges, rules);
    }

    public static void write_model_to_file(String model_filename, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges, ArrayList<ArrayList<RuleObject2>> rules) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(model_filename);
            bw = new BufferedWriter(fw);

            for (int i = 0; i < quantization_levels.size(); i++) {
                bw.write(Integer.toString(quantization_levels.get(i)));
                bw.newLine();
            }
            bw.write("***");
            bw.newLine();

            for (int i = 0; i < ranges.size(); i++) {
                bw.write(i + ":");
                for (int j = 0; j < ranges.get(i).size(); j++) {
                    bw.write(Double.toString(ranges.get(i).get(j)));
                    if (j < ranges.get(i).size() - 1)
                        bw.write(",");
                }
                bw.newLine();
            }
            bw.write("***");
            bw.newLine();

            for (int i = 0; i < rules.size(); i++) {
                bw.write("condition:" + (i+1));
                bw.newLine();

                for (int j = 0; j < rules.get(i).size(); j++) {
                    bw.write(Integer.toString(rules.get(i).get(j).occurence));
                    bw.write(";");
                    for (int k = 0; k < rules.get(i).get(j).rule.length; k++) {
                        bw.write(rules.get(i).get(j).rule[k]);
                        if (k < rules.get(i).get(j).rule.length-1)
                            bw.write(",");
                    }
                    bw.newLine();
                }

                if (i < rules.size()-1) {
                    bw.write("+++");
                    bw.newLine();
                }
            }
            bw.write("+++");
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void print_rules_human_readable(ArrayList<ArrayList<RuleObject2>> rules, ArrayList<String> attribute_types, ArrayList<ArrayList<Double>> ranges) {
        for (int i = 0; i < rules.size(); i++) { // for each condition
            if (rules.get(i).size() > 0) { // if the condition has rules
                System.out.println("Condition: " + (i+1));
                for (int j = 0; j < rules.get(i).size(); j++) { // for each rule
                    System.out.print("IF [ ");
                    for (int k = 0; k < rules.get(i).get(j).rule.length-1; k++) { // for each attribute
                        int feature_pos = Integer.parseInt(rules.get(i).get(j).rule[k].split(":")[0]);

                        if (attribute_types.get(feature_pos).equals("string")) {
                            String feature_value = rules.get(i).get(j).rule[k].split(":")[1];

                            System.out.print("(attribute" + (feature_pos) + " = " + feature_value + ")");
                        }
                        else {
                            int feature_value = (int) Double.parseDouble(rules.get(i).get(j).rule[k].split(":")[1]);

                            double min_range = ranges.get(feature_pos).get(feature_value - 1);
                            double max_range = ranges.get(feature_pos).get(feature_value);

                            System.out.print("(" + min_range + " <= attribute" + (feature_pos) + " < " + max_range + ")");
                        }

                        if (k < (rules.get(i).get(j).rule.length-2))
                            System.out.print(" & ");
                    }
                    System.out.println(" ] -> class: " + rules.get(i).get(j).rule[rules.get(i).get(j).rule.length-1]);
                }
                System.out.println();
            }
        }
    }

    public static void print_examples(ArrayList<Example> examples) {
        for (int i = 0; i < examples.size(); i++) {
            System.out.print(i + ": " + Arrays.toString(examples.get(i).tokens));
            System.out.println(" (" + examples.get(i).hash + ")");
        }
        System.out.println();
    }

    public static void classify_examples(ArrayList<Example> examples_unclassified, ArrayList<RuleObject2> rules_current) {
        for (int i = 0; i < rules_current.size(); i++) { // for each rule
            boolean rule_classifies = false;

            String rule_tokens[] = rules_current.get(i).rule;

            for (int j = 0; j < examples_unclassified.size(); j++) {
                boolean example_classified = true;

                String example_tokens[] = examples_unclassified.get(j).tokens;

                for (int k = 0; k < (rule_tokens.length - 1); k++) {
                    int pos1 = Integer.parseInt(rule_tokens[k].split(":")[0]);
                    String test1 = rule_tokens[k].split(":")[1];
                    String test2 = example_tokens[pos1];

                    if (test2.equals("?") || !test1.equals(test2)) {
                        example_classified = false;
                        break;
                    }
                }

                if (example_classified) {
                    examples_unclassified.remove(j);
                    j--;
                    rule_classifies = true;
                }
            }

            if (!rule_classifies) {
                rules_current.remove(i);
                i--;
            }
        }
    }

    public static void remove_examples_with_different_classes(ArrayList<Example> examples) {
        int pos = 0;
        while (pos < (examples.size() - 1)) {
            boolean repetition_found = false;

            LinkedListArray classes = new LinkedListArray();
            while (examples.get(pos).hash == examples.get(pos+1).hash) { // if features are the same
                repetition_found = true;
                classes.add(examples.get(pos).tokens[examples.get(pos).tokens.length - 1]);
                examples.remove(pos);

                if (pos == (examples.size() - 1))
                    break;
            }

            if (repetition_found) {
                classes.add(examples.get(pos).tokens[examples.get(pos).tokens.length - 1]);
                examples.get(pos).tokens[examples.get(pos).tokens.length - 1] = classes.get(0).text;
            }

            pos++;
        }
    }

    public static ArrayList<Integer> calculate_entropies(int no_of_attributes, ArrayList<Example> examples, boolean verbose) {
        ArrayList<Double> entropies = new ArrayList<>(); // holds entropy values for each attribute
        ArrayList<Integer> entropy_positions = new ArrayList<>();

        for (int i = 0; i < no_of_attributes; i++) { // for each attribute
            //System.out.println("Calculating entropy for Attribute " + (i+1) + "...");

            Map<String, MapObject> instances = new HashMap<>(); // list of unique instances for each attribute

            for (int j = 0; j < examples.size(); j++) { // for each example
                String example_attribute = examples.get(j).tokens[i]; // get the attribute value from example j
                String example_class = examples.get(j).tokens[no_of_attributes]; // get the class from example j

                if (instances.containsKey(example_attribute)) { // if exists
                    instances.get(example_attribute).occurance++;

                    if (instances.get(example_attribute).classes.containsKey(example_class)) {
                        int x = instances.get(example_attribute).classes.get(example_class);
                        x++;
                        instances.get(example_attribute).classes.put(example_class, x);
                    }
                    else
                        instances.get(example_attribute).classes.put(example_class, 1);
                }
                else { // if doesn't exist
                    MapObject temp = new MapObject();
                    temp.classes.put(example_class, 1);
                    instances.put(example_attribute, temp);
                }
            }

            if (verbose) {
                for (String entry : instances.keySet()) {
                    System.out.println(entry + " (" + instances.get(entry).occurance + ")");

                    for (String entry2 : instances.get(entry).classes.keySet())
                        System.out.println("    " + entry2 + " (" + instances.get(entry).classes.get(entry2) + ")");

                    System.out.println();
                }
            }

            double entropy = 0;
            for (String entry : instances.keySet()) {
                double without_rate = 0;
                for (String entry2 : instances.get(entry).classes.keySet()) {
                    double term = instances.get(entry).classes.get(entry2)/((double)instances.get(entry).occurance);
                    without_rate = without_rate + ((-1) * term * (Math.log(term) / Math.log(2)));
                }
                without_rate = without_rate * (instances.get(entry).occurance/((double)examples.size()));
                entropy = entropy + without_rate;
            }
            entropies.add(entropy);
        }

        // Print entropies
        System.out.println("Entropies:");
        for (int i = 0; i < entropies.size(); i++)
            System.out.println(i + " -> [" + entropies.get(i) + "]");
        System.out.println();

        // Sort entropies list
        // Initialize entropy positions array
        for (int i = 0; i < entropies.size(); i++)
            entropy_positions.add(i);

        for (int i = 0; i < entropies.size(); i++) {
            for (int j = i+1; j < entropies.size(); j++) {
                if (entropies.get(j) < entropies.get(i)) {
                    double temp = entropies.get(j);
                    entropies.set(j, entropies.get(i));
                    entropies.set(i, temp);

                    int temp2 = entropy_positions.get(j);
                    entropy_positions.set(j, entropy_positions.get(i));
                    entropy_positions.set(i, temp2);
                }
            }
        }

        return entropy_positions;
    }

    public static void find_ranges(int no_of_attributes, ArrayList<Example> examples, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges) {
        ArrayList<Double> mins = new ArrayList<Double>();
        ArrayList<Double> maxs = new ArrayList<Double>();

        // initialize mins and maxs
        for (int i = 0; i < no_of_attributes; i++) {
            mins.add(Double.MAX_VALUE);
            maxs.add(0.0);
        }

        // Find mins and maxs
        for (int j = 0; j < no_of_attributes; j++) { // for each attribute
            if (quantization_levels.get(j) > 0) {
                for (int i = 0; i < examples.size(); i++) { // for each example
                    if (!examples.get(i).tokens[j].equals("?")) {
                        double x = Double.parseDouble(examples.get(i).tokens[j]);

                        if (x < mins.get(j))
                            mins.set(j, x);

                        if (x > maxs.get(j))
                            maxs.set(j, x);
                    }
                }
            }
        }

        // Calculate ranges
        for (int j = 0; j < no_of_attributes; j++) { // for each attribute
            ArrayList<Double> range = new ArrayList<>();

            if (quantization_levels.get(j) > 0) {
                // get quantization level
                int quantization = 0;
                if (quantization_levels.size() == 1)
                    quantization = quantization_levels.get(0);
                else
                    quantization = quantization_levels.get(j);

                double min = mins.get(j);
                double max = maxs.get(j);

                double difference = (max - min) / (double) quantization;

                range.add(min);
                for (int i = 0; i < (quantization - 1); i++)
                    range.add(min + (difference * (i+1)));
                range.add(max);

                ranges.add(range);
            }
            else
                ranges.add(range);
        }
    }

    public static void set_ranges(ArrayList<Example> examples, ArrayList<Integer> quantization_levels, ArrayList<ArrayList<Double>> ranges, int no_of_attributes) {
        for (int j = 0; j < no_of_attributes; j++) { // for each attribute
            if (quantization_levels.get(j) > 0) {
                for (int i = 0; i < examples.size(); i++) { // for each example
                    if (!examples.get(i).tokens[j].equals("?")) {
                        ArrayList<Double> range = ranges.get(j);
                        boolean found = false;
                        for (int k = 1; k < (range.size() - 1); k++) { // for each range
                            if (Double.parseDouble(examples.get(i).tokens[j]) < range.get(k)) {
                                examples.get(i).tokens[j] = Integer.toString(k);
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                            examples.get(i).tokens[j] = Integer.toString(range.size() - 1);
                    }
                }
            }
        }
    }

    public static void output_arraylist(ArrayList<String> input) {
        System.out.println("OUTPUT");
        System.out.println("======");

        for (int i = 0; i < input.size(); i++) {
            String tokens[] = input.get(i).split(",");
            for (int j = 0; j < tokens.length; j++)
                System.out.print(tokens[j] + "  ");
            System.out.println();
        }

        System.out.println("======");
    }
}

class Example {
    int hash;
    String[] tokens;
}

class LinkedList {
    int occurance = 1;
    String text = "";

    public void increment_occurence() {
        this.occurance++;
    }
}

class MapObject {
    int occurance = 1;
    Map<String, Integer> classes = new HashMap<>();
}

class LinkedListArray {
    ArrayList<LinkedList> list = new ArrayList<>();

    public int size() {
        return list.size();
    }

    public void sort_linkedlist(int j) {
        for (int i = j; i > 0; i--) {
            if (list.get(i).occurance > list.get(i-1).occurance) {
                LinkedList temp = list.get(i);
                list.set(i, list.get(i-1));
                list.set(i-1, temp);
            }
        }
    }

    public void add(String text) {
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).text.equals(text)) {
                LinkedList temp = list.get(i);
                temp.increment_occurence();
                list.set(i, temp);
                sort_linkedlist(i);
                found = true;
                break;
            }
        }

        if (!found) {
            LinkedList temp = new LinkedList();
            temp.text = text;
            list.add(temp);
        }
    }

    public LinkedList get(int i) {
        return list.get(i);
    }
}

class RuleObject {
    String rule_class;
    int occurence;
    Map<Integer, Integer> classifies = new HashMap<>();
}

class RuleObject2 {
    String[] rule;
    int occurence;
    Map<Integer, Integer> classifies = new HashMap<>();
}
