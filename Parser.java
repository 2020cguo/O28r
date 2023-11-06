import com.github.javaparser;

public class Parser {

    public MethodNode getMethodforGivenLineNum(String srcFilePath, int linenum) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);

        char[] fileContent = null;
        try {
            fileContent = getFileContent(srcFilePath).toCharArray();
        } catch (IOException e) {
            System.out.printf("getMethodforGivenLineNum-getFileContent failed!\n%s", srcFilePath);
            e.printStackTrace();
            return null;
        }

        parser.setSource(fileContent);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodNode> methodNodeList = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                SimpleName methodName = node.getName();

                int startLineNum = cu.getLineNumber(node.getStartPosition());

                int endLineNum = cu.getLineNumber(node.getStartPosition() + node.getLength());

                methodNodeList.add(new MethodNode(methodName.toString(), node, startLineNum, endLineNum));
                return false;
            }
        });
    }

    public static void main(String[] args) {
        System.out.print(getMethodforGivenLineNum("./test_runs/Test.java"));
    }
}