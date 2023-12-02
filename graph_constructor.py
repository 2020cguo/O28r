# globals
PATH_LENGTH = []

### builds dependency graph ###
def buildGraph(input_file):
    # input_file = entire dependencies output by java callgraph
    with open(input_file) as f:
        data=f.readlines()

    all_methods = set()
    depGraph = {}

    for line in data:
        if line[0] == "C":
            continue
        pair = line.split()
        dep = pair[1][3:].strip().split("(")[0].split(".")[-1]
        callsIt = pair[0][2:].strip().split("(")[0].split(".")[-1]
        # add dep and callsIt to the set of all methods if not already in there
        if dep not in all_methods:
            all_methods.add(dep)
        if callsIt not in all_methods:
            all_methods.add(callsIt)
        # key: dependency, value: list of methods that call it
        if dep in depGraph.keys():
            depGraph[dep].append(callsIt)
        else:
            depGraph[dep] = [callsIt]

    return depGraph, all_methods

### outputting propagation paths ###
def methodTrace(input_file, depGraph, all_methods):
    # input_file = list of methods to trace (javaparser stuff)
    with open(input_file) as f:
        data=f.readlines()
    
    errors = []
    # line = errClass.errMethod
    for line in data:
        # TODO: remove if no parsing required
        # pair = line.split()
        # errClass = pair[0]
        # errMethod = pair[1]
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
    norm_crit = [round(float(i)*100/max(crit_scores)) for i in crit_scores]
    print(norm_crit)



def findPaths(start, depGraph, vertex_list, vertex_count, visited):
    vertex_count += 1
    vertex_list[vertex_count] = start
    # vertex_list.append(start)
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
    global PATH_LENGTH
    output_str = ""
    actual_list = []
    for x in vertex_list:
        if(x != 0):
            actual_list.append(x)
    PATH_LENGTH.append(len(actual_list))
    print("->".join(actual_list))
    # if len(vertex_list) > 0:
    #     print("->".join([str(x) for x in vertex_list if x != 0]))
    #     PATH_LENGTH.append()

### Function to print a BFS of graph ###
def BFS(s, depGraph):

    # Mark all the vertices as not visited
    visited = {}
    for i in depGraph.keys():
        visited[i] = False

    # Create a queue for BFS
    queue = []

    # Mark the source node as
    # visited and enqueue it
    queue.append(s)
    visited[s] = True

    while queue:
        # Dequeue a vertex from
        # queue and print it
        s = queue.pop(0)
        # print(s, end=" ")

        # Get all adjacent vertices of the
        # dequeued vertex s.
        # If an adjacent has not been visited,
        # then mark it visited and enqueue it
        if s not in depGraph.keys():
            continue
        for i in depGraph[s]:
            if i not in depGraph.keys():
                print(" -->", i, end ="")
                continue
            if visited[i] == False:
                queue.append(i)
                visited[i] = True
                print(" -->",i, end = "")


# thank you candace <3
def main(dependencies, error_methods):
    depGraph, all_methods = buildGraph(dependencies)
    # print("DEPENDENCY GRAPH:", depGraph, "\n")

    methodTrace(error_methods, depGraph, all_methods)

if __name__ == "__main__":
    main()