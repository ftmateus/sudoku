import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class Sudoku {
    public static void main(String[] args) throws IOException {

        InputStream input = System.in;

        if(args.length > 0)
            input = new FileInputStream(new File(args[0]));
        else
            System.out.println("Parsing input from stdin");

        var boards = parseSudokuFile(input);

        for(var board : boards)
        {
            System.out.println();
            
            var s = new SudokuGame(true, false);
            
            s.solveSudoku(board);
            
            s.printBoardSimple();

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
