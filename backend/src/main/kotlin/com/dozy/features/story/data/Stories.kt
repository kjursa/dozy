package com.dozy.features.story.data

internal object Stories {
    val story1 = """
        The Lantern That Lost Its Glow
        {heroes} were walking through {place} when a little lantern coughed and went dark. It had spent all evening guiding beetles home and had forgotten to rest.

        They tucked the lantern into a nest of dry leaves. A beetle brought a crumb of honey cake. A moth told a quiet story about the moon. Soon the lantern started to glow again, but only a tiny bit.

        “That is enough,” said the moth. So they walked home slowly, sharing the lantern’s little circle of light. That night, it dreamed of being a star with nowhere else to go.
    """.trimIndent()

    val story2 = """
        A Pocket Full of Rain
        As {heroes} explored {place}, they found a cloud small enough to sit in a pocket. It was worried because it had never learned where to rain.

        Nearby stood a pot with a drooping mint plant. They set the cloud above the pot and waited. One drop fell, then three more. The mint lifted its leaves, and the air smelled fresh and sweet.

        The cloud was so pleased that it made a mist the size of a teacup. They thanked it for the rain and left it beside the mint, where it curled into a soft grey pillow.
    """.trimIndent()

    val story3 = """
        The Quietest Parade
        {heroes} heard soft tapping in {place}. Around the corner, five snails were trying to hold a parade without waking the hedgehog, who had been up all night baking bread.

        They helped the snails wrap their drums in scarves. Then they put little flags on the snail shells and showed everyone how to clap by tapping two fingers against a palm.

        The parade took almost an hour to cross one path. When the hedgehog finally woke, he could see the flags from his window. He brought out warm rolls, and the snails decided that a slow parade was just right.
    """.trimIndent()

    val story4 = """
        The Moon’s Missing Button
        In {place}, {heroes} found a silver button no bigger than a pea. It shivered each time a cloud passed, and they guessed the moon might be missing it.

        A spider offered a little silk. They made a line and tied the button to a kite. The wind rose just enough to carry it past the highest branch. Up there, a moonbeam caught the button and pulled it home.

        The moon sent down a square of silver light. They spread it across a doorstep so the night creatures could see their way home. By morning, only a cool patch remained.
    """.trimIndent()

    val story5 = """
        A Boat for the Little Leaf
        {heroes} came across a puddle in {place}. A small leaf wanted to see the other side, but it kept getting stuck against the stones.

        They made a boat from a strip of bark and gave it a paper sail. A frog showed them which way the breeze blew. They let the boat go, and it turned twice before sailing straight past the stones.

        On the far bank, they set the leaf under a fern. It had seen all it wanted to see. As the wind died down, the empty boat drifted back and came to rest beside their shoes.
    """.trimIndent()

    private val samples = listOf(story1, story2, story3, story4, story5)

    fun random(): String = samples.random()
}
