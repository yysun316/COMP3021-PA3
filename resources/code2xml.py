import ast
import sys
import lxml.etree as etree

import ast2xml

if __name__ == '__main__':
    # Example AST
    if len(sys.argv) < 2:
        print('Please provide a python file')
        exit(0)
    py_file = sys.argv[1]
    if not py_file.endswith('.py'):
        print('Please provide a python file')
        exit(0)

    with open(py_file, 'r') as f:
        code = f.read()
    try:
        # Parse the code into an AST
        tree = ast.parse(code)
        xml_ast = ast2xml.ast2xml().convert(tree).decode("utf-8")
        element = etree.XML(xml_ast)
        etree.indent(element)
        with open(py_file.replace('.py', '.xml'), 'w') as f:
            f.write(etree.tostring(element, pretty_print=True, encoding='unicode'))
    except Exception as e:
        print(e)
        print('Please provide a valid python file')

