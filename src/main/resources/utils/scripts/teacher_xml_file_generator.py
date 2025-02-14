import os
import random
from xml.etree.ElementTree import Element, SubElement, tostring, ElementTree
from xml.dom import minidom  # For pretty printing

output_directory = "teachers_xml"
os.makedirs(output_directory, exist_ok=True)  # Create directory if it doesn't exist

def pretty_print_xml(element):
    """Helper function to pretty-print XML with line breaks and indentation."""
    rough_string = tostring(element, 'utf-8')
    parsed = minidom.parseString(rough_string)
    return parsed.toprettyxml(indent="    ")

subjects = ["Mathematics", "Science", "History", "Geography", "English", "Art", "Physical Education"]

# Loop to create 50 XML files
for teacher_id in range(101, 151):
    # Create the root element
    teachers = Element("teachers")
    
    # Create the teacher element
    teacher = SubElement(teachers, "teacher")
    
    # Add teacher data
    name = SubElement(teacher, "name")
    name.text = f"Teacher {teacher_id}"
    
    age = SubElement(teacher, "age")
    age.text = str(random.randint(25, 65))
    
    subject = SubElement(teacher, "subject")
    subject.text = random.choice(subjects) 
    
    experience = SubElement(teacher, "experience")
    experience.text = str(random.randint(1, 40))
    
    file_name = f"teacher_{teacher_id}.xml"
    file_path = os.path.join(output_directory, file_name)
    
    pretty_xml = pretty_print_xml(teachers)
    
    # Save formatted XML to file
    with open(file_path, "w", encoding="utf-8") as fh:
        fh.write(pretty_xml)

print(f"Created 50 formatted XML files in the '{output_directory}' folder.")