from random import choice
import gc

import radio
from microbit import *


CLASSIC_STORY = """
Scissors cuts Paper
Paper covers Rock
Rock crushes Scissors
"""


SHELDON_COOPER_STORY = """
Scissors cuts Paper
Paper covers Rock
Rock crushes Lizard
Lizard poisons Spock
Spock smashes Scissors
Scissors decapitates Lizard
Lizard eats Paper
Paper disproves Spock
Spock vaporizes Rock
(and as it always has) Rock crushes Scissors
"""


STORY = SHELDON_COOPER_STORY


# @formatter:off
SKETCHES = {
    'Scissors': '*    :'
                '  *  :'
                '    *:'
                '  *  :'
                '*    :',

    'Paper':    '*****:'
                '*   *:'
                '*   *:'
                '*   *:'
                '*****:',

    'Rock':     ' *** :'
                '*****:'
                '*****:'
                '*****:'
                ' *** :',

    'Lizard':   '**   :'
                '  *  :'
                '  *  :'
                '  *  :'
                '   **:',

    'Spock':    ' *** :'
                '*****:'
                '** **:'
                '** **:'
                '** **:',
}
# @formatter:on


def sketch_to_image(sketch):
    return Image(sketch.replace(' ', '0').replace('*', '9'))


IMAGES = {name: sketch_to_image(sketch) for name, sketch in SKETCHES.items()}


def parse_story(story):
    gc.collect()
    sentences = [line.split()[-3:] for line in story.split('\n') if line.strip()]
    actors = {s for s, v, o in sentences}
    victims = {actor: {o for s, v, o in sentences if s == actor} for actor in actors}
    return actors, victims


ACTORS, VICTIMS = parse_story(STORY)


def random_outcome():
    return choice(list(ACTORS))


def expired(time):
    return abs(running_time() - time) > 2000


def beats(actor, victim):
    return victim in VICTIMS.get(actor, [])


def wait_for_outcome():
    start = running_time()
    while not expired(start):
        received = radio.receive()
        if received:
            return received


def main():
    last_received = 0
    other_outcome = None

    radio.on()

    while True:
        received = radio.receive()
        if received:
            other_outcome = received
            last_received = running_time()

        if not accelerometer.was_gesture('face down'):
            continue

        my_outcome = random_outcome()
        radio.send(my_outcome)

        if not other_outcome or expired(last_received):
            other_outcome = wait_for_outcome()

        if other_outcome:
            sleep(1000)
            image = IMAGES.get(my_outcome, Image.SAD)
            if beats(my_outcome, other_outcome):
                to_show = [Image('00000:'
                                 '00000:'
                                 '00000:'
                                 '00000:'
                                 '00000:'), image, image, image] * 5

            else:
                to_show = [image] * 20

            display.show(to_show, delay=200, clear=True, wait=False)
            other_outcome = None


if __name__ == '__main__':
    main()

