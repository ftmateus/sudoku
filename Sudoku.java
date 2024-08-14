import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Sudoku {
    public static void main(String[] args) throws IOException, InterruptedException {

        InputStream input = System.in;

        if(args.length > 0)
            input = new FileInputStream(new File(args[0]));
        else
            System.out.println("Parsing input from stdin");

        var boards = parseSudokuFile(input);
        
        ExecutorService executor = Executors.newFixedThreadPool(4);

        var results = new LinkedList<SudokuGame>();

        for(var board : boards)
        {
            var s = new SudokuGame(true, false);

            executor.submit(() -> {
                s.solveSudoku(board);
            });

            results.add(s);
            
            // s.printBoardSimple();
        }

        executor.shutdown();

        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        for(var r : results) {
            System.out.println();

            r.printBoardSimple();
            
            System.out.println("#####################");
        }
    }

    public static List<char[][]> parseSudokuFile(InputStream in) throws IOException
    {
        List<char[][]> result = new LinkedList<>();

        try(var reader = new BufferedReader(new InputStreamReader(in)))
        {
            do {
                var board = SudokuGame.parseSudokuBoard(reader);
                if(board != null)
                    result.add(board);
            } while(reader.ready());
        }

        return result;
    }
}
