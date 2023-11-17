### builds dependency graph ###
def buildGraph(input_file):
    # input_file = entire dependencies output by java callgraph
    with open(input_file) as f:
        data=f.readlines()

    depGraph = {}
    for line in data:
        if line[0] == "C":
            continue
        pair = line.split()
        dep = pair[1][3:].strip()
        callsIt = pair[0][2:].strip()
        # key: dependency, value: list of methods that call it
        if dep in depGraph.keys():
            depGraph[dep].append(callsIt)
        else:
            depGraph[dep] = [callsIt]

    return depGraph

### outputting propagation paths ###
def methodTrace(input_file, depGraph):
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

    for err in errors:
        print("REVERSE PROPAGATION PATH FOR", err, end="")
        BFS(err, depGraph)
        print("")


# # A function used by DFS
# def DFSUtil(v, visited, depGraph):

#     # Mark the current node as visited
#     # and print it
#     visited.add(v)
#     print(v, end=' ')

#     # Recur for all the vertices
#     # adjacent to this vertex
#     if v not in depGraph.keys():
#         return
#     for neighbour in depGraph[v]:
#         if neighbour not in visited:
#             DFSUtil(neighbour, visited, depGraph)

    
# # The function to do DFS traversal. It uses
# # recursive DFSUtil()
# def DFS(v, depGraph):

#     # Create a set to store visited vertices
#     visited = set()

#     # Call the recursive helper function
#     # to print DFS traversal
#     DFSUtil(v, visited, depGraph)


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
def main():
    depGraph = buildGraph("graph_constructor_test.txt")
    print("DEPENDENCY GRAPH:", depGraph, "\n")

    methodTrace("graph_constructor_err_methods.txt", depGraph)

if __name__ == "__main__":
    main()