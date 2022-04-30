/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.document;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.datasource.DataSource;

/**
 * Represents a textual document, providing methods to edit it incrementally
 * and address regions of text. A text document delegates IO operations
 * to a {@link TextFile}. It reflects some in-memory snapshot of the file,
 * though the file may still be edited externally.
 *
 * <p>TextDocument is meant to replace CPD's {@link SourceCode} and PMD's
 * {@link DataSource}, though the abstraction level of {@link DataSource}
 * is the {@link TextFile}.
 *
 * <p>Note that the backing {@link TextFile} is purposefully not accessible
 * from a text document. Exposing it here could lead to files being written
 * to from within rules, while we want to eventually build an API that allows
 * file edition based on AST manipulation.
 *
 * <h3>Coordinates in TextDocument</h3>
 *
 * This interface is an abstraction over a piece of text, which might not
 * correspond to the backing source file. This allows the document to
 * be a view on a piece of a larger document (eg, a Javadoc comment, or
 * a string in which a language is injected). Another use case is to perform
 * escape translation, while preserving the line breaks of the original source.
 *
 * <p>This complicates addressing within a text document. To explain it,
 * consider that there is always *one* text document that corresponds to
 * the backing text file, which we call the <i>root</i> text document.
 * Logical documents built on top of it are called <i>views</i>.
 *
 * Text documents use <i>offsets</i> and {@link TextRegion} to address their
 * contents. These are always relative to the {@linkplain #getText() text} of
 * the document. Line and column information are provided by {@link FileLocation}
 * (see {@link #toLocation(TextRegion)}), and are always absolute (ie,
 * represent actual source lines in the file).
 *
 * <p>For instance, say you have the following file (and root text document):
 * <pre>{@code
 * l1
 * l2 (* comment *)
 * l3
 * }</pre>
 * and you create a view for just the section {@code (* comment *)}.
 * Then, that view's offset 0 (start of the document) will map
 * to the {@code (} character, while the root document's offset 0 maps
 * to the start of {@code l1}. When calling {@code toLocation(caretAt(0))},
 * the view will however return {@code line 2, column 4}, ie, a line/column
 * that can be found when inspecting the file.
 *
 * <p>To reduce the potential for mistakes, views do not provide access
 * to their underlying text document. That way, nodes only have access
 * to a single document, and their offsets can be assumed to be in the
 * coordinate system of that document.
 *
 * <p>This interface does not provide a way to obtain line/column
 * coordinates that are relative to a view's coordinate system. This
 * would complicate the construction of views significantly.
 */
public interface TextDocument extends Closeable {
    // todo logical sub-documents, to support embedded languages
    //  ideally, just slice the text, and share the positioner
    //  a problem with document slices becomes reference counting for the close routine


    // todo text edition (there are some reverted commits in the branch
    //  with part of this, including a lot of tests)


    /**
     * Returns the language version that should be used to parse this file.
     */
    LanguageVersion getLanguageVersion();

    /**
     * Returns {@link TextFile#getPathId()} for the text file backing this document.
     */
    String getPathId();

    /**
     * Returns {@link TextFile#getDisplayName()} for the text file backing this document.
     */
    String getDisplayName();


    /**
     * Returns the current text of this document. Note that this doesn't take
     * external modifications to the {@link TextFile} into account.
     *
     * <p>Line endings are normalized to {@link TextFileContent#NORMALIZED_LINE_TERM}.
     *
     * @see TextFileContent#getNormalizedText()
     */
    Chars getText();

    /**
     * Returns a slice of the original text. Note that this is not the
     * same as {@code getText().subsequence}, as if this document has
     * translated escapes, the returned char slice will contain the
     * untranslated escapes, whereas {@link #getText()} would return
     * the translated characters.
     *
     * @param region A region, in the coordinate system of this document
     *
     * @return The slice of the original text that corresponds to the region
     *
     * @throws IndexOutOfBoundsException If the region is not a valid range
     */
    Chars sliceOriginalText(TextRegion region);

    /**
     * Returns a slice of the source text. This is always equal to
     * {@code getText().slice(region)}, as the text is the translated text.
     *
     * @param region A region, in the coordinate system of this document
     *
     * @return The slice of the original text that corresponds to the region
     *
     * @throws IndexOutOfBoundsException If the region is not a valid range
     */
    default Chars sliceTranslatedText(TextRegion region) {
        return getText().slice(region);
    }


    /**
     * Returns a checksum for the contents of the file.
     *
     * @see TextFileContent#getCheckSum()
     */
    long getCheckSum();


    /**
     * Returns the input offset for the given output offset. This maps
     * back an offset in the coordinate system of this document, to the
     * coordinate system of the original document. This includes the
     * length of any unicode escapes.
     *
     * <pre>
     * input:      "a\u00a0b"   (original document)
     * translated: "a b"        (this document)
     *
     * translateOffset(0) = 0
     * translateOffset(1) = 1
     * translateOffset(2) = 7 // includes the length of the escape
     * </pre>
     *
     * @param outOffset Output offset
     * @param inclusive Whether the offset is to be interpreted as the index of a character (true),
     *                  or the position after a character (false)
     *
     * @return Input offset
     */
    int inputOffset(int outOffset, boolean inclusive);

    /**
     * Translate a region given in the coordinate system of this
     * document, to the coordinate system of the original document.
     * This works as if creating a new region with both start and end
     * offsets translated through {@link #inputOffset(int, boolean)}. The
     * returned region may have a different length.
     *
     * @param outputRegion Output region
     *
     * @return Input region
     */
    TextRegion inputRegion(TextRegion outputRegion);

    /**
     * Translate a 2D range given in the coordinate system of this
     * document, to the coordinate system of the original document.
     * This works as if creating a new region with both start and end
     * offsets translated through {@link #inputOffset(int, boolean)}. The
     * returned region may have a different length.
     *
     * @param outputRange Output region
     *
     * @return Input region
     */
    default TextRange2d inputRange(TextRange2d outputRange) {
        return toRange2d(inputRegion(toRegion(outputRange)));
    }


    /**
     * Returns a reader over the text of this document.
     */
    default Reader newReader() {
        return getText().newReader();
    }

    /**
     * Returns the length in characters of the {@linkplain #getText() text}.
     */
    default int getLength() {
        return getText().length();
    }

    /**
     * Returns a text region that corresponds to the entire document,
     * in the coordinate system of this document.
     */
    default TextRegion getEntireRegion() {
        return TextRegion.fromOffsetLength(0, getLength());
    }

    /**
     * Returns a 2D text range that corresponds to the entire document,
     * in the coordinate system of this document.
     */
    default TextRange2d getEntireRegion2d() {
        return toRange2d(getEntireRegion());
    }

    /**
     * Returns a region that spans the text of all the given lines.
     * This is intended to provide a replacement for {@link SourceCode#getSlice(int, int)}.
     *
     * <p>Note that, as line numbers may only be obtained from {@link #toLocation(TextRegion)},
     * and hence are line numbers of the original source, both parameters
     * must be line numbers of the source text and not the translated text
     * that this represents.
     *
     * @param startLineInclusive Inclusive start line number (1-based)
     * @param endLineInclusive   Inclusive end line number (1-based)
     *
     * @throws IndexOutOfBoundsException If the arguments do not identify
     *                                   a valid region in the source document
     */
    TextRegion createLineRange(int startLineInclusive, int endLineInclusive);


    /**
     * Turn a text region into a {@link FileLocation}. This computes
     * the line/column information for both start and end offset of
     * the region.
     *
     * @param region A region, in the coordinate system of this document
     *
     * @return A new file position, with absolute coordinates
     *
     * @throws IndexOutOfBoundsException If the argument is not a valid region in this document
     */
    FileLocation toLocation(TextRegion region);

    /**
     * Turn a text region into a {@link FileLocation}. The file name is
     * the display name of this document.
     *
     * @return A new file position
     *
     * @throws IndexOutOfBoundsException If the argument is not a valid region in this document
     */
    default FileLocation toLocation(TextRange2d range) {
        int startOffset = offsetAtLineColumn(range.getStartPos());
        if (startOffset < 0) {
            throw new IndexOutOfBoundsException("Region out of bounds: " + range.displayString());
        }
        TextRegion region = TextRegion.caretAt(startOffset);
        checkInRange(region, this.getLength());
        return FileLocation.range(getDisplayName(), range);
    }

    /**
     * Turn a text region to a {@link TextRange2d}.
     */
    default TextRange2d toRange2d(TextRegion region) {
        TextPos2d start = lineColumnAtOffset(region.getStartOffset(), true);
        TextPos2d end = lineColumnAtOffset(region.getEndOffset(), false);
        return TextRange2d.range2d(start, end);
    }

    /**
     * Turn a {@link TextRange2d} into a {@link TextRegion}.
     */
    default TextRegion toRegion(TextRange2d region) {
        return TextRegion.fromBothOffsets(
            offsetAtLineColumn(region.getStartPos()),
            offsetAtLineColumn(region.getEndPos())
        );
    }


    /**
     * Returns the offset at the given line and column number.
     *
     * @param line   Line number (1-based)
     * @param column Column number (1-based)
     *
     * @return an offset (0-based)
     */
    int offsetAtLineColumn(int line, int column);

    /**
     * Returns true if the position is valid in this document.
     */
    boolean isInRange(TextPos2d textPos2d);

    /**
     * Returns the offset at the line and number.
     */
    default int offsetAtLineColumn(TextPos2d pos2d) {
        return offsetAtLineColumn(pos2d.getLine(), pos2d.getColumn());
    }

    /**
     * Returns the line and column at the given offset (inclusive).
     * Note that the line/column cannot be converted back. They are
     * absolute in the coordinate system of the original document.
     *
     * @param offset A source offset (0-based), can range in {@code [0, length]}.
     *
     * @throws IndexOutOfBoundsException if the offset is out of bounds
     */
    default TextPos2d lineColumnAtOffset(int offset) {
        return lineColumnAtOffset(offset, true);
    }

    /**
     * Returns the line and column at the given offset.
     * Both the input offset and the output range are in the coordinates
     * of this document.
     *
     * @param offset    A source offset (0-based), can range in {@code [0, length]}.
     * @param inclusive If the offset falls right after a line terminator,
     *                  two behaviours are possible. If the parameter is true,
     *                  choose the position at the start of the next line,
     *                  otherwise choose the position at the end of the line.
     *
     * @return A position, in the coordinate system of the root document
     *
     * @return A position, in the coordinate system of this document
     *
     * @throws IndexOutOfBoundsException if the offset is out of bounds
     */
    TextPos2d lineColumnAtOffset(int offset, boolean inclusive);


    /**
     * Closing a document closes the underlying {@link TextFile}.
     * New editors cannot be produced after that, and the document otherwise
     * remains in its current state.
     *
     * @throws IOException           If {@link TextFile#close()} throws
     * @throws IllegalStateException If an editor is currently open. In this case
     *                               the editor is rendered ineffective before the
     *                               exception is thrown. This indicates a programming
     *                               mistake.
     */
    @Override
    void close() throws IOException;

    /**
     * Create a new text document for the given text file. The document's
     * coordinate system is the same as the original text file.
     *
     * @param textFile A text file
     *
     * @return A new text document
     *
     * @throws IOException          If the file cannot be read ({@link TextFile#readContents()})
     * @throws NullPointerException If the parameter is null
     */
    static TextDocument create(TextFile textFile) throws IOException {
        return new RootTextDocument(textFile);
    }

    /**
     * Returns a read-only document for the given text.
     *
     * @see TextFile#forCharSeq(CharSequence, String, LanguageVersion)
     */
    static TextDocument readOnlyString(final CharSequence source, LanguageVersion lv) {
        return readOnlyString(source, TextFile.UNKNOWN_FILENAME, lv);
    }

    /**
     * Returns a read-only document for the given text. This works as
     * if by calling {@link TextDocument#create(TextFile)} on a textfile
     * produced by {@link TextFile#forCharSeq(CharSequence, String, LanguageVersion) forString},
     * but doesn't throw {@link IOException}, as such text files will
     * not throw.
     *
     * @see TextFile#forCharSeq(CharSequence, String, LanguageVersion)
     */
    @SuppressWarnings("PMD.CloseResource")
    static TextDocument readOnlyString(@NonNull CharSequence source, @NonNull String filename, @NonNull LanguageVersion lv) {
        TextFile textFile = TextFile.forCharSeq(source, filename, lv);
        try {
            return create(textFile);
        } catch (IOException e) {
            throw new AssertionError("String text file should never throw IOException", e);
        }
    }

}
