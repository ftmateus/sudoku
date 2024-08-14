import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

class SudokuGame {

    
    public static final int SUDOKU_SIZE = 9;

    private static final byte SUDOKU_N_CELLS = SUDOKU_SIZE*SUDOKU_SIZE;

    private byte cellsLeft = SUDOKU_N_CELLS;

    private boolean[][] rowsNumbers     = new boolean[SUDOKU_SIZE][SUDOKU_SIZE];
    private byte[] rowsNumbersCount     = new byte[SUDOKU_SIZE];

    private boolean[][] columnsNumbers  = new boolean[SUDOKU_SIZE][SUDOKU_SIZE];
    private byte[] columnsNumbersCount  = new byte[SUDOKU_SIZE];

    private boolean[][] regionsNumbers  = new boolean[SUDOKU_SIZE][SUDOKU_SIZE];
    private byte[] regionsNumbersCount  = new byte[SUDOKU_SIZE];

    static final int QUEUE_PRIORITY_LEVELS = 3;

    private int initialFreeCellsNumber;

    private boolean boardAnalysed = false;

    // boolean backtracking = false;

    static boolean leetcodeJudging = true;

    static boolean useMultithreading = false;

    private SudokuCell[][] cellPool = new SudokuCell[SUDOKU_SIZE][SUDOKU_SIZE];


    private Deque<SudokuCell>[] cellPriorityQueue = new Deque[QUEUE_PRIORITY_LEVELS]; 
    {
        for(int l = 0; l < QUEUE_PRIORITY_LEVELS; l++)
            cellPriorityQueue[l] = new LinkedList<>();
    }

    private char[][] board;

    public SudokuGame() {}


    public static char[][] parseSudokuBoard(BufferedReader reader) throws IOException
    {
        char[][] result = new char[SUDOKU_SIZE][SUDOKU_SIZE];

        // try(var reader = new BufferedReader(new InputStreamReader(in))) {

        for(int i = 0; i < SUDOKU_SIZE; i++)
        {
            String line; do {
                line = reader.readLine();
                if(line == null)
                    return null;
                line = line.trim();
            } while(line.length() < SUDOKU_SIZE || line.charAt(0) == '#'); 

            //skip tabs, spaces, colons, semicolons, curly and rect brackets, and sigle and double quotes 
            result[i] = line.replaceAll("\t|\s|,|;|\\{|\\}|\\[|\\]|'|\"|\\|", "")
                .substring(0, 9)
                .toCharArray();
        }
        // }

        return result;
    }

    public SudokuGame(boolean leetcodeJudging, boolean useMultithreading)
    {
        this.leetcodeJudging = leetcodeJudging;
        this.useMultithreading = useMultithreading;
    }

    public SudokuGame(char[][] board)
    {
        this.leetcodeJudging = true;
        this.useMultithreading = false;
        this.board = board;
    }

    public void solveSudoku() {
        this.solveSudoku(this.board);
    }


    public void solveSudoku(char[][] board)
    {
        this.board = board;

        try {
            analyseBoard();

            solveBoard();
        } 
        catch(SudokuException se)
        {
            System.err.println(se.getMessage());
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        // backtracking = true;
        // solveBoardBacktracking();

        return;
    }

    private boolean solveBoardBacktracking() throws InterruptedException
    {
        if(!leetcodeJudging)
        {
            printBoard();
            System.out.println("Start backtracking... Cells left: " + cellsLeft);
        }

        // if(cellsLeft >= 60)
        //     Collections.sort((List) cellPriorityQueue[2]);

        if(cellsLeft >= 60 && useMultithreading)
            return solveBoardBacktrackingMultiThread();
        else
            return solveBoardBacktrackingSingleThread(true);
    }

    private char[][] copyBoard(char[][] board)
    {
        char[][] newBoard = new char[SUDOKU_SIZE][SUDOKU_SIZE];
        for(int i = 0; i < SUDOKU_SIZE; i++)
            newBoard[i] = Arrays.copyOf(board[i], SUDOKU_SIZE);

        return newBoard;
    }

    private boolean solveBoardBacktrackingMultiThread() throws InterruptedException
    {
        SudokuCell cell = popCellWithMostPossibilities();
        assert cell != null;
        assert cell.getPossibleNumbersCount() > 1;

        int nThreads = cell.getPossibleNumbersCount();

        List<Thread> threads = new ArrayList<>(nThreads + 1);
        List<char[][]> threadsBoards = new ArrayList<>(nThreads + 1);

        boolean[] threadsResults = new boolean[nThreads]; 

        int currentThread = 0;
        for(int n = 0; n < SUDOKU_SIZE; n++)
        {
            if(!cell.isNumberPossible(n)) continue;

            char[][] threadBoard = copyBoard(board);

            threadBoard[cell.row][cell.column] = (char) (n + '0' + 1);

            threadsBoards.add(threadBoard);

            Thread t = new Thread(
                new Runnable() {

                    int threadId = currentThread;

                    @Override
                    public void run() {
                        var s = new SudokuGame(false, false);

                        s.solveSudoku(threadBoard);

                        threadsResults[threadId] = s.cellsLeft == 0;
                    }
                }
            );

            t.start();

            threads.add(t);
        }

        for(Thread t : threads)
            t.join();

        return false;
    }

    

    private boolean solveBoardBacktrackingSingleThread(final boolean firstBacktrackingLevel) throws InterruptedException
    {
        // boolean firstBacktrackingLevel = firstLevel;
        // if(!backtracking)
        //     firstBacktrackingLevel = true;
        assert cellPriorityQueue[0].isEmpty();

        SudokuCell cell = popQueue();
        if(cell == null) 
        {
            assert cellsLeft == 0;
            return true;
        }
        assert !cell.isSolved(board);
        assert !cell.hasAttempt(board);
        assert cell.getPossibleNumbersCount() > 1;

        int numbersTested = 0;
        for(int n = 0; n < SUDOKU_SIZE; n++)
        {
            if(!cell.isNumberPossible(n))
                continue;

            if(firstBacktrackingLevel && numbersTested > 0)
            {
                if(cell.getPossibleNumbersCount() == 1)
                {
                    solveCell(cell);
                    assert cell.isSolved(board);
                    return solveBoard();
                }
                else
                {
                    analyseRowOneOccurences(cell.row);
                    analyseColumnOneOccurences(cell.column);
                    analyseRegionOneOccurences(cell.region);
                    if(cellPriorityQueue[0].size() > 0)
                    {
                        addToQueue(cell, -1);
                        return solveBoard();
                    }
                }
            }
    
            if(!attemptSolveCell(cell, n)) 
            {
                assert !cell.hasAttempt(board);
                assert !firstBacktrackingLevel;
                continue;
            }
            assert cell.hasAttempt(board);

            boolean successful = solveBoardBacktrackingSingleThread(false);
            if(successful) return true;

            undoCell(cell);

            assert !cell.hasAttempt(board);

            if(firstBacktrackingLevel)
                cell.markNumberAsNotPossible(n);

            numbersTested++;
            assert numbersTested > 0 && numbersTested <= SUDOKU_SIZE;
        }

        assert !firstBacktrackingLevel;
        
        addTryCellBackToQueue(cell);
        
        assert getQueueSize() == cellsLeft : getQueueSize() + " " + cellsLeft;

        return false;
    }

    private boolean solveBoard() throws InterruptedException
    {
        while(cellsLeft > 0)
        {
            assert getQueueSize() == cellsLeft;
            SudokuCell cell = popQueueFirstLevel();

            if(cell == null)
            {
                findOneOccurrences();

                if(cellPriorityQueue[0].isEmpty())
                    return solveBoardBacktracking();
            }
            else
            {
                solveCell(cell);
                assert cell.isSolved(board);
            }

            assert sudokuSanityCheck();
        }
        assert cellsLeft == 0;
        return true;
    }

    private void findOneOccurrences() {
        for(int i = 0; i < SUDOKU_SIZE; i++)
        {
            analyseRowOneOccurences(i);
            analyseColumnOneOccurences(i);
            analyseRegionOneOccurences(i);
        }
    }

    private boolean sudokuSanityCheck()
    {
        if(leetcodeJudging) return true;

        byte[] rowsCount = new byte[SUDOKU_SIZE];
        byte[] columnsCount = new byte[SUDOKU_SIZE];
        byte[] regionsCount = new byte[SUDOKU_SIZE];
        byte occupiedCells = 0;

        for(int r = 0; r < SUDOKU_SIZE; r++)
        {
            for(int c = 0; c < SUDOKU_SIZE; c++)
            {
                SudokuCell cell = getSudokuCell(r, c);
                final int n = cell.getNumber(board);

                if(n == SudokuCell.FREE_CELL)
                {
                    assert cell.getPossibleNumbersCount() > 0;
                    continue; 
                }

                assert rowsNumbers[r][n] == true;
                assert columnsNumbers[c][n] == true;
                assert regionsNumbers[cell.region][n] == true;
                rowsCount[r]++;
                columnsCount[c]++;
                regionsCount[cell.region]++;
                occupiedCells++;       
                        
            }
        }

        assert occupiedCells == SUDOKU_N_CELLS - this.cellsLeft;

        for(int i = 0; i < SUDOKU_SIZE; i++)
        {
            assert rowsCount[i] == this.rowsNumbersCount[i];
            assert columnsCount[i] == this.columnsNumbersCount[i];
            assert regionsCount[i] == this.regionsNumbersCount[i];
        }

        return true;
    }

    private void analyseBoard()
    {
        assert !boardAnalysed;
        if(board.length != SUDOKU_SIZE)
            throw new SudokuException("Invalid board!");

        for(int l = 0; l < SUDOKU_SIZE; l++)
        {
            if(board[l].length != SUDOKU_SIZE)
                throw new SudokuException("Invalid board!");

            for(int c = 0; c < SUDOKU_SIZE; c++)
                createAndAnalyseCell(l, c);
        }

        for(int l = 0; l < SUDOKU_SIZE; l++)
            for(int c = 0; c < SUDOKU_SIZE; c++)
            {
                SudokuCell cell = getSudokuCell(l, c);
                boolean validCell = analyseCellPossibleNumbers(cell);
                if(!validCell)
                    throw new SudokuException("Invalid board!");
            }

        assert getQueueSize() == cellsLeft;
        assert cellsLeft >= 0 && cellsLeft <= SUDOKU_N_CELLS;
        initialFreeCellsNumber = cellsLeft;

        boardAnalysed = true;
    }

    private void analyseRowOneOccurences(int row)
    {
        if(rowsNumbersCount[row] == SUDOKU_SIZE) return;

        for(int n = 0; n < SUDOKU_SIZE; n++)
        {
            if(rowsNumbers[row][n] == true) continue;
            int occurences = 0;
            SudokuCell occurencePos = null;
            for(int c = 0; c < SUDOKU_SIZE; c++)
            {
                if(columnsNumbersCount[c] == SUDOKU_SIZE) continue;
                var cell = getSudokuCell(row, c);
                if(cell.isNumberPossible(n))
                {
                    occurencePos = cell;
                    occurences++;
                }
                if(occurences == 2)
                    break;
            }

            if(occurences == 1)
            {
                assert occurencePos != null;
                processOneOccurence(occurencePos, n);
            }
            else if(occurences == 0)
                throw new SudokuException("Board has no solutions!");
        }
    }

    private void analyseColumnOneOccurences(int column)
    {
        if(columnsNumbersCount[column] == SUDOKU_SIZE) return;

        for(int n = 0; n < SUDOKU_SIZE; n++)
        {
            if(columnsNumbers[column][n] == true) continue;
            int occurences = 0;
            SudokuCell occurencePos = null;
            for(int l = 0; l < SUDOKU_SIZE; l++)
            {
                if(rowsNumbersCount[l] == SUDOKU_SIZE) continue;
                var cell = getSudokuCell(l, column);
                if(cell.isNumberPossible(n))
                {
                    occurencePos = cell;
                    occurences++;
                }
                if(occurences == 2)
                    break;
            }

            if(occurences == 1)
            {
                assert occurencePos != null;
                processOneOccurence(occurencePos, n);
            }
            else if(occurences == 0)
                throw new SudokuException("Board has no solutions!");
        }
    }

    private void processOneOccurence(SudokuCell cell, int num)
    {
        if(cell.getPossibleNumbersCount() == 1) return;
        int oldCount = cell.getPossibleNumbersCount();
        for(int i = 0; i < SUDOKU_SIZE; i++)
        {
            if(i != num)
                cell.markNumberAsNotPossible(i);
            else
                assert cell.isNumberPossible(num);
        }

        assert cell.getPossibleNumbersCount() == 1;
        cell.oneOccurence = true;
        
        addToQueue(cell, oldCount);
    }

    private void analyseRegionOneOccurences(int region)
    {
        if(regionsNumbersCount[region] == SUDOKU_SIZE) return;

        int rl = region/3;
        int rc = region%3;
                        
        for(int n = 0; n < SUDOKU_SIZE; n++)
        {
            if(regionsNumbers[region][n] == true) continue;
            int occurences = 0;
            SudokuCell occurencePos = null;
            for(int l = rl*3; l < rl*3 + 3 && occurences < 2; l++)
            {
                if(rowsNumbersCount[l] == SUDOKU_SIZE) continue;
                for(int c = rc*3; c < rc*3 + 3; c++)
                {
                    if(columnsNumbersCount[c] == SUDOKU_SIZE) continue;
                    var cell = getSudokuCell(l, c);
                    if(cell.isNumberPossible(n))
                    {
                        occurencePos = cell;
                        occurences++;
                    }
                    if(occurences == 2)
                        break;
                }
            }
            if(occurences == 1)
            {
                assert occurencePos != null;
                processOneOccurence(occurencePos, n);
            }
            else if(occurences == 0)
                throw new SudokuException("Board has no solutions!");
        }
    }

    private boolean analyseCell(SudokuCell cell)
    {
        assert cell != null;
        return analyseCell(cell.row, cell.column);
    }

    private boolean analyseCell(int row, int column)
    {
        // final int n = board[row][column] - '0' - 1;
        // if(n >= SUDOKU_SIZE || n < 0) 
        //     return true;

        SudokuCell cell = getSudokuCell(row, column);

        if(cell.isFree(board)) 
            return true;

        final int n = cell.getNumber(board);

        boolean validCell = 
            rowsNumbers[cell.row][n] == columnsNumbers[cell.column][n]
        &&  columnsNumbers[cell.column][n] == regionsNumbers[cell.region][n]
        &&  regionsNumbers[cell.region][n] == rowsNumbers[cell.row][n];

        if(!validCell)
            return false;

        assert rowsNumbers[cell.row][n] == false;
        rowsNumbers[cell.row][n] = true;
        rowsNumbersCount[cell.row]++;
        assert rowsNumbersCount[cell.row] <= SUDOKU_SIZE 
            && rowsNumbersCount[cell.row] >= 1;
    
        assert columnsNumbers[cell.column][n] == false;
        columnsNumbers[cell.column][n] = true;
        columnsNumbersCount[cell.column]++;
        assert columnsNumbersCount[cell.column] <= SUDOKU_SIZE 
            && columnsNumbersCount[cell.column] >= 1;

        assert regionsNumbers[cell.region][n] == false;
        regionsNumbers[cell.region][n] = true;
        regionsNumbersCount[cell.region]++;
        assert regionsNumbersCount[cell.region] <= SUDOKU_SIZE 
            && regionsNumbersCount[cell.region] >= 1;
        

        cellsLeft--;
        assert cellsLeft >= 0 && cellsLeft <= SUDOKU_N_CELLS;

        return true;
    }

    private boolean analyseCellPossibleNumbers(SudokuCell cell)
    {
        assert cell != null;

        if(cell.isSolved(board))
            return true;

        if(cell.oneOccurence)
        {
            assert cell.getPossibleNumbersCount() == 1;   
            return true;
        }
                    
        int previousCellPossibleNumbersCount = cell.getPossibleNumbersCount();
    
        for(int n = 0; n < SUDOKU_SIZE; n++)
        {
            if( rowsNumbers[cell.row][n] == true
            ||  columnsNumbers[cell.column][n] == true
            ||  regionsNumbers[cell.region][n] == true)
                cell.markNumberAsNotPossible(n);
            else
                cell.markNumberAsPossible(n);
        }
        
        assert !boardAnalysed 
        ||     cell.getPossibleNumbersCount() <= previousCellPossibleNumbersCount;

        if(cell.getPossibleNumbersCount() == 0)
            return false;

        addToQueue(cell, previousCellPossibleNumbersCount);

        return true;
    }

    private void addToQueue(SudokuCell cell, int previousCellPossibleNumbersCount)
    {
        // assert !backtracking || cellPriorityQueue[0].isEmpty();
        assert cell != null;
        assert !cell.isSolved(board);
        assert !cell.hasAttempt(board);
        assert previousCellPossibleNumbersCount <= 0 
        || cell.getPossibleNumbersCount() <= previousCellPossibleNumbersCount;

        int previousQueueLevel = possibleNumbersToQueueLevel(
            previousCellPossibleNumbersCount 
        );

        int queueLevel = possibleNumbersToQueueLevel(
            cell.getPossibleNumbersCount() 
        );
        
        if(cell.getPossibleNumbersCount() == previousCellPossibleNumbersCount) {
            assert previousQueueLevel == queueLevel;
            assert leetcodeJudging || cellPriorityQueue[queueLevel].contains(cell);   
            return;
        }

        if(queueLevel == previousQueueLevel) {
            assert leetcodeJudging || cellPriorityQueue[queueLevel].contains(cell);   
            return;
        }

        if(previousCellPossibleNumbersCount > 0)
        {
            boolean removed = cellPriorityQueue[
                possibleNumbersToQueueLevel(
                    previousCellPossibleNumbersCount
                )
            ].removeIf(n -> n == cell);

            assert removed;
        }
        else 
            assert leetcodeJudging || !isCellInQueue(cell);

        
        
        // if (queueLevel == 2 && hasLessPossibleNumbersThanLastQueueFirstCell(cell))
        //     cellPriorityQueue[2].addFirst(cell);
        // else
            cellPriorityQueue[queueLevel].addLast(cell);
    }

    private boolean hasLessPossibleNumbersThanLastQueueFirstCell(SudokuCell cell)
    {
        var firstQ = cellPriorityQueue[2].peek();

        if(firstQ == null) return true;

        return cell.getPossibleNumbersCount() <= firstQ.getPossibleNumbersCount();
    }

    private void addTryCellBackToQueue(SudokuCell cell)
    {
        assert leetcodeJudging || !isCellInQueue(cell);

        int queueLevel = possibleNumbersToQueueLevel(
            cell.getPossibleNumbersCount() 
        );

        cellPriorityQueue[queueLevel].addFirst(cell);
    }

    private SudokuCell popQueueLevel(int level)
    {
        // assert !backtracking || cellPriorityQueue[0].isEmpty();

        assert level >= 0 && level < QUEUE_PRIORITY_LEVELS;
        if(cellPriorityQueue[level].isEmpty())
            return null;
        
        SudokuCell cell = cellPriorityQueue[level].pop();

        assert !cell.isSolved(board);
        assert !cell.hasAttempt(board);
        assert cell.getPossibleNumbersCount() > 0;

        return cell;
    }
    
    private SudokuCell popQueueFirstLevel()
    {
        SudokuCell cell =  popQueueLevel(0);

        assert cell == null || cell.getPossibleNumbersCount() == 1;

        return cell;
    }

    private SudokuCell popQueue()
    {
        for(int l = 0; l < QUEUE_PRIORITY_LEVELS; l++)
        {
            SudokuCell cell = popQueueLevel(l);
            if(cell != null) return cell;
        }
        return null;
    }

    private SudokuCell popCellWithMostPossibilities()
    {
        assert useMultithreading;

        int largestCp = 0;
        SudokuCell cellPos = null;

        int l = QUEUE_PRIORITY_LEVELS - 1;
        for(; l >= 0; l--)
        {
            cellPos = Collections.max(cellPriorityQueue[l]);
            // for(SudokuCell c : cellPriorityQueue[l])
            // {
            //     if(c.getPossibleNumbersCount() > largestCp)
            //     {
            //         cellPos = c;
            //         largestCp = c.getPossibleNumbersCount();
            //         if(largestCp == SUDOKU_SIZE) break;
            //     }
            // }
            if(cellPos != null) break;
        }
        assert cellPos != null;

        boolean cellRemoved = cellPriorityQueue[l].remove(cellPos);
        assert cellRemoved;

        return cellPos;
    }

    private int getQueueSize()
    {
        return cellPriorityQueue[0].size() 
            + cellPriorityQueue[1].size() 
            + cellPriorityQueue[2].size();
    }

    private boolean isCellInQueue(SudokuCell cell)
    {
        for(int l = QUEUE_PRIORITY_LEVELS - 1; l >= 0; l--)
            if(cellPriorityQueue[l].contains(cell))
                return true;

        return false;
    }

    private void updateAffectedCells(SudokuCell cell)
    {
        assert cell != null;
        for(int r = 0; r < SUDOKU_SIZE; r++)
        {
            if(r == cell.row) continue;

            var rCell = getSudokuCell(r, cell.column);
            boolean valid = analyseCellPossibleNumbers(rCell);
            if(!valid)
                throw new SudokuException("Board has no solutions!");
        }

        for(int c = 0; c < SUDOKU_SIZE; c++)
        {
            if(c == cell.column) continue;

            var cCell = getSudokuCell(cell.row, c);
            boolean valid = analyseCellPossibleNumbers(cCell);
            if(!valid)
                throw new SudokuException("Board has no solutions!");
        }

        int rl = cell.region/3;
        int rc = cell.region%3;
        for(int l = rl*3; l < rl*3 + 3; l++)
        {
            if(l == cell.row) continue;
            for(int c = rc*3; c < rc*3 + 3; c++)
            {
                if(c == cell.column) continue;
                var rCell = getSudokuCell(l, c);
                boolean valid = analyseCellPossibleNumbers(rCell);
                if(!valid)
                    throw new SudokuException("Board has no solutions!");
            }
        }
    }

    private void solveCell(SudokuCell cell)
    {
        assert cell != null;
        assert cell.getPossibleNumbersCount() == 1;
        int n = SUDOKU_SIZE - 1;
        //should always be the last possibility
        for(; n >= 0; n--)
            if(cell.isNumberPossible(n))
                break;

        board[cell.row][cell.column] = (char) ('0' + n + 1);

        boolean validCell = analyseCell(cell);
        if(!validCell)
            throw new SudokuException("Board has no solutions!");

        cell.markNumberAsNotPossible(n);
        assert cell.getPossibleNumbersCount() == 0;
        assert cell.isSolved(board);

        updateAffectedCells(cell);
    }

    private boolean attemptSolveCell(SudokuCell cell, int n)
    {
        assert cell != null;
        assert cell.getPossibleNumbersCount() > 1;
        assert !cell.isSolved(board);
        assert !cell.hasAttempt(board);

        if(rowsNumbers[cell.row][n] == true
        || columnsNumbers[cell.column][n] == true
        || regionsNumbers[cell.region][n] == true)
            return false;

        board[cell.row][cell.column] = (char) ('0' + n + 1);

        boolean validCell = analyseCell(cell);
        assert validCell;

        assert cell.hasAttempt(board);

        return true;
    }

    private void undoCell(SudokuCell cell)
    {
        assert cell != null;
        assert !cell.isSolved(board);
        assert cell.hasAttempt(board);
        
        int n = board[cell.row][cell.column] - '0' - 1;
        assert n < SUDOKU_SIZE && n >= 0;

        board[cell.row][cell.column] = '.';
        
        rowsNumbers[cell.row][n] = false;
        rowsNumbersCount[cell.row]--;
        assert rowsNumbersCount[cell.row] <= SUDOKU_SIZE 
            && rowsNumbersCount[cell.row] >= 0;

        columnsNumbers[cell.column][n] = false;
        columnsNumbersCount[cell.column]--;
        assert columnsNumbersCount[cell.column] <= SUDOKU_SIZE 
            && columnsNumbersCount[cell.column] >= 0;

        regionsNumbers[cell.region][n] = false;
        regionsNumbersCount[cell.region]--;
        assert regionsNumbersCount[cell.region] <= SUDOKU_SIZE 
            && regionsNumbersCount[cell.region] >= 0;

        cellsLeft++;       

        assert cellsLeft >= 0 && cellsLeft <= initialFreeCellsNumber; 
        assert !cell.hasAttempt(board);
    }

    private int possibleNumbersToQueueLevel(int possibleNumbers)
    {   
        assert possibleNumbers >= 0 && possibleNumbers <= SUDOKU_SIZE;

        if(possibleNumbers <= 2)
            return possibleNumbers - 1;
            
        else return 2;
    }

    private void printBoard()
    {
        System.out.print("{");
        for(int l = 0; l < SUDOKU_SIZE; l++)
        {
            System.out.print("{");
            for(int c = 0; c < SUDOKU_SIZE; c++)
            {
                System.out.print("\'" + board[l][c] + "\'");
                if(c < SUDOKU_SIZE - 1)
                    System.out.print(',');
                if(l == SUDOKU_SIZE - 1 && c == SUDOKU_SIZE - 1)
                    System.out.print("}");
                
                if((c + 1)%3 == 0 && c < SUDOKU_SIZE - 1)
                    System.out.print("\t");
            }
            System.out.print("}");
            if((l + 1)%3 == 0)
                System.out.println();
            System.out.println();
            if(l < SUDOKU_SIZE - 1)
                System.out.print(',');
        }
       
    }

    public void printBoardSimple()
    {
        for(int l = 0; l < SUDOKU_SIZE; l++)
        {
            for(int c = 0; c < SUDOKU_SIZE; c++)
            {
                System.out.print(board[l][c]);
                if(c < SUDOKU_SIZE - 1)
                    System.out.print(' ');
                
                if((c + 1)%3 == 0 && c < SUDOKU_SIZE - 1)
                    System.out.print("\t");
            }
            if((l + 1)%3 == 0)
                System.out.println();
            System.out.println();
        }
       
    }

    private SudokuCell createAndAnalyseCell(int row, int column)
    {
        assert !boardAnalysed;
        assert row >= 0 && row < SUDOKU_SIZE;
        assert column >= 0 && column < SUDOKU_SIZE;

        assert cellPool[row][column] == null;

        final int n = board[row][column] - '0' - 1;
        boolean isClue = n >= 0 && n < SUDOKU_SIZE;

        cellPool[row][column] = new SudokuCell((byte) row, (byte) column, isClue);

        boolean validCell = analyseCell(cellPool[row][column]);
        if(!validCell)
            throw new SudokuException("Invalid board!");

        return cellPool[row][column];

    }

    private SudokuCell getSudokuCell(int row, int column)
    {
        assert row >= 0 && row < SUDOKU_SIZE;
        assert column >= 0 && column < SUDOKU_SIZE;

        assert cellPool[row][column] != null;
        
        // if(cellPool[row][column] != null) 
        return cellPool[row][column];

        // cellPool[row][column] = new SudokuCell((byte) row, (byte) column);

        // return cellPool[row][column];
    }

    private static class SudokuException extends RuntimeException {

        public SudokuException(String message) {
            super(message);
        }
        
    }
}