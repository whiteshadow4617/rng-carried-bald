package dev.arena.spiderdropalert;

/** Sea-creature announcements counted by the Fishing tracker. */
enum LegendaryCreature {
    THE_LOCH_EMPEROR("The Loch Emperor", "THE LOCH EMPEROR ARISES FROM THE DEPTHS",
            "THE SEA EMPEROR", "SEA EMPEROR", "A SEA EMPEROR"),
    WATER_HYDRA("Water Hydra", "THE WATER HYDRA HAS COME TO TEST YOUR STRENGTH",
            "THE WATER HYDRA HAS COME"),
    ALLIGATOR("Alligator", "A LONG SNOUT BREAKS THE SURFACE OF THE WATER"),
    ABYSSAL_MINER("Abyssal Miner", "AN ABYSSAL MINER BREAKS OUT OF THE WATER",
            "AN ABYSSAL MINER BREAKS"),
    BLUE_RINGED_OCTOPUS("Blue Ringed Octopus", "A GARISH SET OF TENTACLES ARISE"),
    FIERY_SCUTTLER("Fiery Scuttler", "A FIERY SCUTTLER INCONSPICUOUSLY WADDLES UP TO YOU"),
    GREAT_WHITE_SHARK("Great White Shark", "HIDE NO LONGER, A GREAT WHITE SHARK HAS TRACKED YOUR SCENT",
            "HIDE NO LONGER, A GREAT WHITE SHARK"),
    PHANTOM_FISHER("Phantom Fisher", "THE SPIRIT OF A LONG LOST PHANTOM FISHER"),
    PUDDLE_JUMPER("Puddle Jumper", "A PUDDLE JUMPER IS PREPARING FOR LIFTOFF"),
    SILKBREEZE("Silkbreeze", "SOMETHING ZIPS THROUGH THE AIR"),
    THUNDER("Thunder", "YOU HEAR A MASSIVE RUMBLE AS THUNDER EMERGES",
            "YOU HEAR A MASSIVE RUMBLE AS"),
    YETI("Yeti", "WHAT IS THIS CREATURE"),
    LORD_JAWBUS("Lord Jawbus", "YOU HAVE ANGERED A LEGENDARY CREATURE... LORD JAWBUS HAS ARRIVED");

    final String displayName;
    final String chatPrefix;
    final String[] chatPrefixes;

    LegendaryCreature(String displayName, String chatPrefix, String... aliases) {
        this.displayName = displayName;
        this.chatPrefix = chatPrefix;
        this.chatPrefixes = new String[aliases.length + 1];
        this.chatPrefixes[0] = chatPrefix;
        System.arraycopy(aliases, 0, this.chatPrefixes, 1, aliases.length);
    }
}
