#TODO: account for no empty try catches
def parse(input_file, output_file):
    output_lines = []
    with open(input_file) as f:
        data=f.readlines()
    for line in data:
        line_arr = line.split(":")
        if line_arr[2] == "\tEmptyCatchBlock":
            output_lines.append(line_arr[1])
    
    with open(output_file, 'w') as f:
        for line in output_lines:
            f.write(line+"\n")

