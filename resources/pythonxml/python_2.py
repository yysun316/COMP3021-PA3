def toggle_light(turn_on: bool):
    if turn_on:
        state = True
        print("The light is now ON.")
    else:
        state = False
        print("The light is now OFF.")