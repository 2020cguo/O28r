import os

# dir_name = "parser_output"

#TODO: account for no empty try catches
def parse(input_file, output_dir):
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
            file_name = k.replace("/", "%") + "%Output.txt"
            with open(output_dir + "/" + file_name, 'w') as f:
                for line in file_map[k]:
                    f.write(line+"\n")
