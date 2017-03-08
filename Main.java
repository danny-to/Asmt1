package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

/**
 * @author Danny To
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Scene scene = new Scene(new Group());

        BorderPane main_window = new BorderPane();
        TableView table = new TableView();
        ToolBar toolbar = new ToolBar();
        main_window.setTop(toolbar);
        main_window.setCenter(table);

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("."));
        File mainDirectory = directoryChooser.showDialog(primaryStage);

        File[] train_spam_files = new File(mainDirectory.getAbsolutePath() + "/train/spam/").listFiles();
        File[] train_ham_files = new File(mainDirectory.getAbsolutePath() + "/train/ham/").listFiles();

        Double spam_threshold = 0.5;

        //Maps that will store the occurences of each unique word
        Map<String, Integer> train_spam_freq = new HashMap<String, Integer>();
        Map<String, Integer> train_ham_freq = new HashMap<String, Integer>();

        for(File file: train_spam_files){
            ArrayList<String> file_words = file_read(file); //gets all unique words from file
            for(String new_word: file_words){
                if (!train_spam_freq.containsKey(new_word)){
                    train_spam_freq.put(new_word, 1); //starts new entry if word is new
                } else{
                    train_spam_freq.replace(new_word, train_spam_freq.get(new_word)+1);
                }
            }
        }

        for(File file: train_ham_files){
            ArrayList<String> file_words = file_read(file);
            for(String new_word: file_words){
                if (!train_ham_freq.containsKey(new_word)){
                    train_ham_freq.put(new_word, 1);
                } else{
                    train_ham_freq.replace(new_word, train_ham_freq.get(new_word)+1);
                }
            }
        }

        Map<String, Double> spam_prob = new HashMap<String, Double>();
        Map<String, Double> ham_prob = new HashMap<String, Double>();
        Map<String, Double> file_prob = new HashMap<String, Double>();

        //calculates frequency of words in their respective sets and adds them to
        //file_prob, which is the set of P(S|Wi) values
        for(String word: train_spam_freq.keySet()){
            spam_prob.put(word, (double)train_spam_freq.get(word) /
                    train_spam_files.length);
        }

        for(String word: train_ham_freq.keySet()){
            ham_prob.put(word, (double)train_ham_freq.get(word) /
                    train_ham_files.length);
        }

        for(String word: spam_prob.keySet()){
            if(ham_prob.containsKey(word)){
                file_prob.put(word, spam_prob.get(word) / (spam_prob.get(word) +
                        ham_prob.get(word)));
            } else{
                file_prob.put(word, 1.0);
            }
        }

        File[] test_spam_files = new File(mainDirectory.getAbsolutePath() + "/test/spam/").listFiles();
        File[] test_ham_files = new File(mainDirectory.getAbsolutePath() + "/test/ham/").listFiles();

        //creating observable list for output later
        ObservableList<TestFile> table_output = FXCollections.observableArrayList();

        int true_pos = 0;
        int false_pos = 0;

        for(File file: test_spam_files){
            ArrayList<String> file_words = file_read(file);
            Double eta = 0.0;
            for(String word: file_words){
                if (file_prob.containsKey(word)){ //if the word has a spam probability associated with it
                    if((file_prob.get(word) != 0.0) & (file_prob.get(word) != 1.0)){//skips words that are invalid
                        eta += Math.log(1.0-file_prob.get(word)) -
                                Math.log(file_prob.get(word)); //calculation
                    }
                }
            }

            Double prob = 1.0 / (1.0 + Math.pow(Math.E, eta));
            if(prob > spam_threshold){ //decides if email is spam
                true_pos++;
            }

            table_output.add(new TestFile(file.getName(), prob, "Spam"));
        }

        for(File file: test_ham_files){
            ArrayList<String> file_words = file_read(file);
            Double eta = 0.0;
            for(String word: file_words){
                if (file_prob.containsKey(word)){
                    if((file_prob.get(word) != 0.0) & (file_prob.get(word) != 1.0)){
                        eta += Math.log(1.0-file_prob.get(word)) -
                                Math.log(file_prob.get(word));
                    }
                }
            }

            Double prob = 1.0 / (1.0 + Math.pow(Math.E, eta));
            if(prob > spam_threshold){
                false_pos++;
            }

            table_output.add(new TestFile(file.getName(), prob, "Ham"));
        }

        Double accuracy = (true_pos + test_ham_files.length - false_pos) / (double)
                (test_spam_files.length + test_ham_files.length);

        Double precision = true_pos / (double)(true_pos + false_pos);

        TableColumn filename_col = new TableColumn("Filename");
        TableColumn actual_class_col = new TableColumn("Actual Class");
        TableColumn prob_col = new TableColumn("Spam Probability");

        filename_col.prefWidthProperty().bind(table.widthProperty().divide(3));
        actual_class_col.prefWidthProperty().bind(table.widthProperty().divide(3));
        prob_col.prefWidthProperty().bind(table.widthProperty().divide(3));

        filename_col.setCellValueFactory(new PropertyValueFactory<TestFile, String>("filename"));
        actual_class_col.setCellValueFactory(new PropertyValueFactory<TestFile, String>("actualClass"));
        prob_col.setCellValueFactory(new PropertyValueFactory<TestFile, Double>("spamProbability"));

        table.setItems(table_output);
        table.getColumns().addAll(filename_col, actual_class_col, prob_col);

        Label accuracy_label = new Label("Accuracy: " + accuracy.toString());
        Label precision_label = new Label("Precision: " + precision.toString());

        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));
        vbox.getChildren().addAll(accuracy_label, precision_label, table);

        ((Group) scene.getRoot()).getChildren().addAll(vbox);
        primaryStage.setWidth(500);
        primaryStage.setScene(scene);
        primaryStage.show();

    }



    public static void main(String[] args){
        launch(args);

    }

    /**
     * Returns an ArrayList of unique words in a file.
     * @param file
     * @return ArrayList of Strings
     */
    public static ArrayList<String> file_read(File file){

        ArrayList<String> file_words = new ArrayList<String>();
        try {
            Scanner sc = new Scanner(file);

            while(sc.hasNext()){
                String word = sc.next();
                if (!file_words.contains(word)){
                    file_words.add(word);
                }
            }

            sc.close();
        } catch(FileNotFoundException e){
            System.out.println("Error");
            System.exit(0);
        }

        return file_words;
    }

}
