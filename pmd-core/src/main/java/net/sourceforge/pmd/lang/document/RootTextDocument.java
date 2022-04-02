/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.document;

import java.io.IOException;
import java.util.Objects;

import net.sourceforge.pmd.internal.util.BaseCloseable;
import net.sourceforge.pmd.lang.LanguageVersion;


/**
 * A text document directly backed by a {@link TextFile}. In the future
 * some other implementations of the interface may be eg views on part
 * of another document.
 */
final class RootTextDocument extends BaseCloseable implements TextDocument {

    private final TextFile backend;


    // to support CPD with the same api, we could probably just store
    // a soft reference to the contents, and build the positioner eagerly.
    private final TextFileContent content;

    private final LanguageVersion langVersion;

    private final String fileName;
    private final String pathId;

    RootTextDocument(TextFile backend) throws IOException {
        this.backend = backend;
        this.content = backend.readContents();
        this.langVersion = backend.getLanguageVersion();
        this.fileName = backend.getDisplayName();
        this.pathId = backend.getPathId();

        Objects.requireNonNull(langVersion, "Null language version for file " + backend);
        Objects.requireNonNull(fileName, "Null display name for file " + backend);
        Objects.requireNonNull(pathId, "Null path id for file " + backend);
    }

    @Override
    public LanguageVersion getLanguageVersion() {
        return langVersion;
    }

    @Override
    public String getDisplayName() {
        return fileName;
    }

    @Override
    public String getPathId() {
        return pathId;
    }

    @Override
    protected void doClose() throws IOException {
        backend.close();
    }

    @Override
    public FileLocation toLocation(TextRegion region) {
        checkInRange(region);
        SourceCodePositioner positioner = content.getPositioner();

        // We use longs to return both numbers at the same time
        // This limits us to 2 billion lines or columns, which is FINE
        long bpos = positioner.lineColFromOffset(region.getStartOffset(), true);
        long epos = region.isEmpty() ? bpos
                                     : positioner.lineColFromOffset(region.getEndOffset(), false);

        return new FileLocation(
            fileName,
            SourceCodePositioner.unmaskLine(bpos),
            SourceCodePositioner.unmaskCol(bpos),
            SourceCodePositioner.unmaskLine(epos),
            SourceCodePositioner.unmaskCol(epos),
            region
        );
    }

    @Override
    public int offsetAtLineColumn(int line, int column) {
        SourceCodePositioner positioner = content.getPositioner();
        return positioner.offsetFromLineColumn(line, column);
    }

    @Override
    public TextPos2d lineColumnAtOffset(int offset) {
        long longPos = content.getPositioner().lineColFromOffset(offset, true);
        return TextPos2d.pos2d(
            SourceCodePositioner.unmaskLine(longPos),
            SourceCodePositioner.unmaskCol(longPos)
        );
    }

    @Override
    public TextRegion createLineRange(int startLineInclusive, int endLineInclusive) {
        SourceCodePositioner positioner = content.getPositioner();

        if (!positioner.isValidLine(startLineInclusive)
            || !positioner.isValidLine(endLineInclusive)
            || startLineInclusive > endLineInclusive) {
            throw invalidLineRange(startLineInclusive, endLineInclusive, positioner.getLastLine());
        }

        int first = positioner.offsetFromLineColumn(startLineInclusive, 1);
        int last = positioner.offsetOfEndOfLine(endLineInclusive);
        return TextRegion.fromBothOffsets(first, last);
    }

    void checkInRange(TextRegion region) {
        if (region.getEndOffset() > getLength()) {
            throw regionOutOfBounds(region.getStartOffset(), region.getEndOffset(), getLength());
        }
    }

    @Override
    public TextFileContent getContent() {
        return content;
    }

    @Override
    public Chars sliceText(TextRegion region) {
        return getText().subSequence(region.getStartOffset(), region.getEndOffset());
    }

    private static final String NOT_IN_RANGE = "Region [start=%d, end=%d[ is not in range of this document (length %d)";
    private static final String INVALID_LINE_RANGE = "Line range %d..%d is not in range of this document (%d lines) (line numbers are 1-based)";

    static IndexOutOfBoundsException invalidLineRange(int start, int end, int numLines) {
        return new IndexOutOfBoundsException(String.format(INVALID_LINE_RANGE, start, end, numLines));
    }

    static IndexOutOfBoundsException regionOutOfBounds(int start, int end, int maxLen) {
        return new IndexOutOfBoundsException(String.format(NOT_IN_RANGE, start, end, maxLen));
    }
}
