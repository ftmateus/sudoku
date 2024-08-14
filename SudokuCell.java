public class SudokuCell implements Comparable<SudokuCell> {

    public static final int FREE_CELL = -1;

    public final byte row;
    public final byte column;
    public final byte region;
    public boolean oneOccurence;
    public final boolean isClue;       
    
    private final boolean[] possibleNumbers;
    private byte possibleNumbersCount;
   
    public SudokuCell(byte row, byte column, boolean isClue)
    {
        this.row = row;
        this.column = column;
        this.isClue = isClue;
        this.oneOccurence = false;
        this.region = (byte) ((row/3)*3 + (column/3)%3);
        assert region >= 0 && region < SudokuGame.SUDOKU_SIZE; 

        this.possibleNumbers = new boolean[SudokuGame.SUDOKU_SIZE];
        this.possibleNumbersCount = 0;
    }

    public boolean isNumberPossible(int number)
    {
        return possibleNumbers[number];
    }
    
    private boolean possibleNumbersSanityCheck()
    {
        assert possibleNumbersCount >= 0 && possibleNumbersCount <= SudokuGame.SUDOKU_SIZE;

        // if(leetcodeJudging) return true;
        // int p = IntStream.range(0, SudokuGame.SUDOKU_SIZE)
        //     .reduce(0, (a, c) -> c + (possibleNumbers[a] ? 1 : 0));
        int p = 0;
        for (int i = 0; i < SudokuGame.SUDOKU_SIZE; i++)
            p += possibleNumbers[i] ? 1 : 0;
        return possibleNumbersCount == p;
    }

    public void markNumberAsPossible(int number)
    {
        assert !isClue;
        boolean alreadyPossible = possibleNumbers[number];
        if(alreadyPossible) return;

        possibleNumbers[number] = true;
        possibleNumbersCount++;
        assert possibleNumbersSanityCheck();
    }

    public void markNumberAsNotPossible(int number)
    {
        assert !isClue;
        boolean alreadyNotPossible = !possibleNumbers[number];
        if(alreadyNotPossible) return;

        possibleNumbers[number] = false;
        possibleNumbersCount--;
        assert possibleNumbersSanityCheck();
    }

    public int getPossibleNumbersCount()
    {
        return possibleNumbersCount;
    }

    public boolean hasAttempt(char[][] board)
    {
        boolean hasAttempt = getNumber(board) != FREE_CELL && possibleNumbersCount > 0;
        assert !isClue || !hasAttempt; //Implication: isClue => !hasAttempt;

        return hasAttempt; 
    }

    public boolean isSolved(char[][] board)
    {
        boolean isSolved = (getNumber(board) != FREE_CELL && possibleNumbersCount == 0);
        assert !isClue || isSolved; //Implication: isClue => isSolved

        return isSolved;
    }

    public boolean isFree(char[][] board)
    {
        boolean isFree = getNumber(board) == FREE_CELL;
        assert !isClue || !isFree; //Implication: isClue => !isFree;

        return isFree;
    }

    public int getNumber(char[][] board)
    {
        final int n = board[row][column] - '0' - 1;

        if(n < 0 || n >= SudokuGame.SUDOKU_SIZE)
            return FREE_CELL;
        else 
            return n;
    }

    @Override
    public int compareTo(SudokuCell other) {
        return this.getPossibleNumbersCount() - other.getPossibleNumbersCount();
    }
}
