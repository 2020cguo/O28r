import os

def parse(input_file, output_dir):
    """
    Reads in txt file containing PMD output,
    identifies lines with empty catch blocks,
    and writes those line numbers to files.

	:param input_file: (String) txt filename, contains PMD output
    :param output_dir: (String) path to output directory
	"""
    file_map = {}
    if not os.path.exists(output_dir):
        os.mkdir(output_dir)

    with open(input_file) as f:
        data=f.readlines()
    for line in data:
        line_arr = line.split(":")
        if line_arr[0] not in file_map.keys():
            file_map[line_arr[0]] = []
        if line_arr[2] == "\tEmptyCatchBlock":
            file_map[line_arr[0]].append(line_arr[1])
    
    for k in file_map.keys():
        if len(file_map[k]) >= 1:
            file_name_arr = k.split("/")[1:]
            file_name = "%".join(file_name_arr) + "&Output.txt"
            with open(output_dir + "/" + file_name, 'w') as f:
                for line in file_map[k]:
                    f.write(line+"\n")
