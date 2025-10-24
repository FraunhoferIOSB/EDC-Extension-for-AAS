import json
import sys

def panic(key):
    print(key)
    return False

def contains(container: dict, contained: dict):
    # Check whether container contains contained

    if isinstance(contained, str):
        return isinstance(container, str) and contained == container

    if isinstance(contained, list):
        if not isinstance(contained, list):
            return panic(key)
        for list_item in contained:
            if isinstance(list_item, str):
                if list_item not in container:
                    return panic(list_item)
            return any([contains(ctr_item, list_item) for ctr_item in container])

        return True


    for key in contained.keys():
        if key not in container:
            return panic(key)

        if isinstance(contained[key], dict):
            if not isinstance(container[key], dict):
                return panic(key)
            if not contains(container[key],contained[key]):
                return panic(key)

        elif isinstance(contained[key], list):
            if not isinstance(container[key], list):
                return panic(key)

            for list_item in contained[key]:
                if list_item not in container[key]:
                    return panic(key)
        
        elif isinstance(contained[key], str):
            return isinstance(container[key], str) and contained[key]==container[key]

    return True



if __name__ == "__main__":
    contained_ptr = sys.argv[1]
    container_ptr = sys.argv[2]

    contained_dict = json.load(open(contained_ptr))
    container_dict = json.load(open(container_ptr))

    if contains(container_dict, contained_dict):
        exit(0)
    exit(1)

