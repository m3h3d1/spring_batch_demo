import os
from xml.etree.ElementTree import Element, SubElement, tostring, ElementTree
from xml.dom import minidom  # For pretty printing

output_directory = "students_xml"
os.makedirs(output_directory, exist_ok=True)

def pretty_print_xml(element):
    """Helper function to pretty-print XML with line breaks and indentation."""
    rough_string = tostring(element, 'utf-8')
    parsed = minidom.parseString(rough_string)
    return parsed.toprettyxml(indent="    ")

# Loop to create 50 XML files
for student_id in range(101, 151):
    # Create the root element
    students = Element("students")
    
    # Create the student element
    student = SubElement(students, "student")
    
    # Add student data
    name = SubElement(student, "name")
    name.text = f"Student {student_id}"
    
    age = SubElement(student, "age")
    age.text = str(random.randint(15, 26))
    
    grade = SubElement(student, "grade")
    grade.text = "A"  # Assuming default grade is A
    
    file_name = f"student_{student_id}.xml"
    file_path = os.path.join(output_directory, file_name)
    
    pretty_xml = pretty_print_xml(students)
    
    # Save formatted XML to file
    with open(file_path, "w", encoding="utf-8") as fh:
        fh.write(pretty_xml)

print(f"Created 50 formatted XML files in the '{output_directory}' folder.")