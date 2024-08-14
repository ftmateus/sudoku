import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Sudoku {

    public static final int GAMES_PER_THREAD = 8;
    public static void main(String[] args) throws IOException, InterruptedException {
        InputStream input = System.in;
        // ExecutorService executor = Executors.newWorkStealingPool(4);
        ExecutorService executor = Executors.newWorkStealingPool();

        if(args.length > 0)
            input = new FileInputStream(new File(args[0]));
        else
            System.out.println("Parsing input from stdin");

        var results = new LinkedList<SudokuGame>();
        
        try(var reader = new BufferedReader(new InputStreamReader(input)))
        {
            while(reader.ready()) {
                var workerGames = new LinkedList<SudokuGame>();

                for(int i = 0; i < GAMES_PER_THREAD; i++)
                {
                    var board = SudokuGame.parseSudokuBoard(reader);
                    if(board == null)
                        continue;
                    
                    var game = new SudokuGame(board);

                    results.add(game);
                    workerGames.add(game);
                }

                if(workerGames.isEmpty())
                    continue;

                executor.execute(() -> {
                    for(var game : workerGames) {
                        game.solveSudoku();
                    }
                });
            }
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
