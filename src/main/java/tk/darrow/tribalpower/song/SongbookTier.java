package tk.darrow.tribalpower.song;

/** How many pages a book holds, and the longest sheet it is willing to keep. */
public enum SongbookTier {
    FIRST(1, 3),
    BOUND(3, 5),
    CHORUS(5, 7);

    public final int pages;
    public final int longest;

    SongbookTier(int pages, int longest) {
        this.pages = pages;
        this.longest = longest;
    }

    public boolean accepts(int reagents) {
        return reagents >= SongVerse.MIN_SHEET && reagents <= longest;
    }
}
