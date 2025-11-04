import json
import sys


def panic(key):
    problems.append(key)
    print("Panicking:", key)
    return False


def contains(container: dict, contained: dict, do_panic: bool = True):
    # Check whether container contains contained
    if isinstance(contained, str):
        return isinstance(container, str) and contained == container

    if isinstance(contained, list):
        if not isinstance(container, list):
            print("container not a list")
            return panic(key) if do_panic else False

        for contained_item in contained:
            if not any([contains(ctr_item, contained_item, do_panic=False) for ctr_item in container]):
                return panic(contained_item) if do_panic else False

    elif isinstance(contained, dict):
        if not isinstance(container, dict):
            print("container not a dict")
            return panic(key) if do_panic else False
        # Now it can only be a dict
        for key in contained.keys():
            if key not in container:
                print("dict item not found")
                return panic(key) if do_panic else False

            if not contains(container[key], contained[key], do_panic=do_panic):
                return panic(key) if do_panic else False

    else:
        print("Unknown json construct")
        return False

    return True


if __name__ == "__main__":
    problems = []

    contained_ptr = sys.argv[1]
    container_ptr = sys.argv[2]

    contained_dict = json.load(open(contained_ptr))
    container_dict = json.load(open(container_ptr))

    if contains(container_dict, contained_dict):
        exit(0)

    with open(f"{container_ptr}_problems.log", "w+") as problem_fd:
        problem_fd.write("Problems:\n")
        problem_fd.writelines(problems)
    exit(1)
