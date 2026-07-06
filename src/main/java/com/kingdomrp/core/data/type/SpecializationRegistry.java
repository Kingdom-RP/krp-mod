package com.kingdomrp.core.data.type;

import java.util.*;

public class SpecializationRegistry {

    private static final Map<String, Specialization> ALL = new LinkedHashMap<>();
    private static final Map<Path, List<Specialization>> BY_PATH = new HashMap<>();

    static {
        // Ремесло
        register(new Specialization("carpenter",   "Плотник",       "Эффективнее работает с деревом и деревянными крафтами.",        Path.CRAFT));
        register(new Specialization("blacksmith",  "Кузнец",        "Эффективнее переплавляет руды, крафтит инструменты и броню.",   Path.CRAFT));
        register(new Specialization("craftsman",   "Мастеровой",    "Работает с натуральными материалами: кожей, шерстью, глиной и керамикой.", Path.CRAFT));

        // Промысел
        register(new Specialization("farmer",      "Фермер",        "Эффективнее выращивает и собирает урожай.",                    Path.HARVEST));
        register(new Specialization("fisherman",   "Рыбак",         "Эффективнее ловит рыбу и добывает морские ресурсы.",           Path.HARVEST));
        register(new Specialization("cook",        "Повар",         "Открывает доступ к приготовлению всё более сытных блюд.",      Path.HARVEST));

        // Добыча
        register(new Specialization("miner",       "Шахтёр",        "Эффективнее добывает руды и камень.",                          Path.MINING));
        register(new Specialization("lumberjack",  "Лесоруб",       "Эффективнее рубит деревья и обрабатывает древесину.",          Path.MINING));

        // Война
        register(new Specialization("warrior",     "Воин",          "Увеличивает урон в ближнем бою и сопротивляемость к урону.",   Path.WAR));
        register(new Specialization("archer",      "Лучник",        "Увеличивает урон из лука и арбалета.",                        Path.WAR));

        // Магия
        register(new Specialization("alchemist",   "Алхимик",       "Открывает доступ к зельям и экономит реагенты при варке.",    Path.MAGIC));
        register(new Specialization("enchanter",   "Зачарователь",  "Эффективнее зачаровывает инструменты, оружие и броню.",       Path.MAGIC));
    }

    private static void register(Specialization spec) {
        ALL.put(spec.id(), spec);
        BY_PATH.computeIfAbsent(spec.path(), k -> new ArrayList<>()).add(spec);
    }

    public static List<Specialization> getForPath(Path path) {
        return BY_PATH.getOrDefault(path, Collections.emptyList());
    }

    public static Optional<Specialization> get(String id) {
        return Optional.ofNullable(ALL.get(id));
    }

    public static int getCount(Path path) {
        return BY_PATH.getOrDefault(path, Collections.emptyList()).size();
    }
}