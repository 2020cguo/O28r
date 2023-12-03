### GLOBALS ###
PATH_LENGTH = []

### METHODS ###
def buildGraph(input_file):
    """
    Reads in txt file containing Java CallGraph output
    and builds a graph out of method dependencies.

	:param input_file: (String) txt filename, contains Java CallGraph output
    :returns: 
        - (Dict of String: List) dependency graph with callee -> callers relationships
        - (Set) all methods identified by Java CallGraph
	"""
    with open(input_file) as f:
        data=f.readlines()

    all_methods = set()
    depGraph = {}

    for line in data:
        if line[0] == "C":
            continue
        pair = line.split()
        dep = pair[1][3:].strip().split("(")[0].split(".")[-1]
        caller = pair[0][2:].strip().split("(")[0].split(".")[-1]
        if dep not in all_methods:
            all_methods.add(dep)
        if caller not in all_methods:
            all_methods.add(caller)
        if dep in depGraph.keys():
            depGraph[dep].append(caller)
        else:
            depGraph[dep] = [caller]

    return depGraph, all_methods


def methodTrace(input_file, depGraph, all_methods):
    """
    Traces dependency paths and outputs number of paths,
    longest path, and criticality score.

	:param input_file: (String) txt filename, contains JavaParser output
    :param depGraph: (Dict of String: List) dependency graph with callee -> callers relationships
    :param all_methods: (Set) all methods identified by Java CallGraph
	"""    
    with open(input_file) as f:
        data=f.readlines()
    
    errors = []
    for line in data:
        errors.append(line.strip())
    crit_scores = []
    traced_errors = set()
    for err in errors:
        if err in traced_errors:
            continue
        else:
            traced_errors.add(err)
        global PATH_LENGTH
        visited = {}
        for method in all_methods:
            visited[method] = False
        vertex_list = [0]*len(all_methods)
        vertex_count = 0
        print("REVERSE PROPAGATION PATH FOR", err)
        findPaths(err, depGraph, vertex_list, vertex_count, visited)
        print("Number of paths:", len(PATH_LENGTH))
        print("Longest path:", max(PATH_LENGTH))
        crit_scores.append(sum(PATH_LENGTH))
        PATH_LENGTH = []
        print()
    norm_crit = [i*100/max(crit_scores) for i in crit_scores]
    print(norm_crit)


def findPaths(start, depGraph, vertex_list, vertex_count, visited):
    """
    Recursively traverses the dependency graph 
    to generate all propagation paths for a method.

	:param start: (String) current vertex
    :param depGraph: (Dict of String: List) dependency graph with callee -> callers relationships
    :param vertex_list: (List) vertices in current path
    :param vertex_count: (int) number of vertices in current path
    :param visited: (Set) visited vertices
	"""
    vertex_count += 1
    vertex_list[vertex_count] = start
    visited[start] = True
    if start not in depGraph.keys():
        printList(vertex_list)
        return
    next_v = depGraph[start]
    flag = 0

    for v in next_v:
        if not visited[v]:
            flag = 1
            findPaths(v, depGraph, vertex_list, vertex_count, visited)
    if flag == 0:
        printList(vertex_list)
    visited[start] = False
    vertex_list.pop()
    vertex_count -= 1


def printList(vertex_list):
    """
    Prints dependency path and adds current 
    path length to global var PATH_LENGTH.

	:param vertex_list: (List) vertices in dependency path
	"""
    global PATH_LENGTH
    output_str = ""
    actual_list = []
    for x in vertex_list:
        if(x != 0):
            actual_list.append(x)
    PATH_LENGTH.append(len(actual_list))
    print("->".join(actual_list))


def main(dependencies, error_methods):
    """
    Builds dependency graph and trace propagation paths.

	:param dependencies: (String) txt filename, contains Java CallGraph output
    :param error_methods: (String) txt filename, contains JavaParser output
	"""
    depGraph, all_methods = buildGraph(dependencies)
    methodTrace(error_methods, depGraph, all_methods)

if __name__ == "__main__":
    main()