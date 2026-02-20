package app.partsvibe.catalog.config;

import app.partsvibe.catalog.domain.Category;
import app.partsvibe.catalog.domain.Part;
import app.partsvibe.catalog.domain.Tag;
import app.partsvibe.catalog.domain.TagColor;
import app.partsvibe.catalog.repo.CategoryRepository;
import app.partsvibe.catalog.repo.PartRepository;
import app.partsvibe.catalog.repo.TagRepository;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogDataInitializer implements ApplicationRunner {
    private final CategoryRepository categoryRepository;
    private final PartRepository partRepository;
    private final TagRepository tagRepository;

    public CatalogDataInitializer(
            CategoryRepository categoryRepository, PartRepository partRepository, TagRepository tagRepository) {
        this.categoryRepository = categoryRepository;
        this.partRepository = partRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tag microcontroller = upsertTag(
                "Microcontroller", TagColor.BLUE, "Compact programmable controller boards for embedded projects.");
        Tag sbc = upsertTag(
                "Single-Board-Computer",
                TagColor.INDIGO,
                "Linux-capable computers with CPU, RAM and I/O on a single board.");
        Tag wifi = upsertTag("WiFi", TagColor.CYAN, "Built-in wireless networking support.");
        Tag bluetooth = upsertTag("Bluetooth", TagColor.TEAL, "Built-in Bluetooth or BLE connectivity.");
        Tag beginnerFriendly =
                upsertTag("Beginner-Friendly", TagColor.GREEN, "Good first board for learning and prototyping.");
        Tag highPerformance = upsertTag(
                "High-Performance",
                TagColor.ORANGE,
                "Suitable for heavier workloads, advanced edge/AI tasks or desktop-like usage.");

        Category arduino = upsertCategory(
                "Arduino",
                "Arduino boards focused on microcontroller-based prototyping, education and embedded development.",
                Set.of(microcontroller, wifi, beginnerFriendly));
        Category raspberryPi = upsertCategory(
                "RaspberryPI",
                "Raspberry Pi boards covering SBC and microcontroller platforms for education and production.",
                Set.of(sbc, microcontroller, wifi, bluetooth, beginnerFriendly));

        upsertPart(
                "Arduino UNO R4 WiFi",
                "32-bit Renesas RA4M1 board with onboard ESP32-S3 for Wi-Fi/Bluetooth and a 12x8 LED matrix.",
                arduino,
                Set.of(microcontroller, wifi, bluetooth, beginnerFriendly));
        upsertPart(
                "Arduino Nano ESP32",
                "Nano form factor board based on ESP32-S3 with native Wi-Fi and Bluetooth for IoT projects.",
                arduino,
                Set.of(microcontroller, wifi, bluetooth, beginnerFriendly));
        upsertPart(
                "Arduino Mega 2560 Rev3",
                "ATmega2560-based board with 54 digital I/O, 16 analog inputs and four hardware serial ports.",
                arduino,
                Set.of(microcontroller));
        upsertPart(
                "Arduino Nano Every",
                "Small 45x18mm board with ATmega4809, designed for low-cost and compact embedded builds.",
                arduino,
                Set.of(microcontroller, beginnerFriendly));
        upsertPart(
                "Arduino Portenta H7",
                "Dual-core STM32H747 platform with high-end features including Wi-Fi/Bluetooth and AI-capable workflows.",
                arduino,
                Set.of(microcontroller, wifi, bluetooth, highPerformance));

        upsertPart(
                "Raspberry Pi 5",
                "Latest flagship SBC with quad-core Cortex-A76 CPU and significantly higher performance than prior generations.",
                raspberryPi,
                Set.of(sbc, wifi, bluetooth, highPerformance));
        upsertPart(
                "Raspberry Pi 4 Model B",
                "Popular quad-core SBC with dual-display support, Gigabit Ethernet and broad community ecosystem.",
                raspberryPi,
                Set.of(sbc, wifi, bluetooth, beginnerFriendly));
        upsertPart(
                "Raspberry Pi Zero 2 W",
                "Tiny low-cost board with built-in 2.4GHz Wi-Fi and Bluetooth, ideal for compact IoT use-cases.",
                raspberryPi,
                Set.of(sbc, wifi, bluetooth));
        upsertPart(
                "Raspberry Pi 400",
                "Keyboard-integrated personal computer built on Raspberry Pi silicon for desktop-style usage.",
                raspberryPi,
                Set.of(sbc, wifi, bluetooth, beginnerFriendly));
        upsertPart(
                "Raspberry Pi Pico 2 W",
                "Microcontroller board based on RP2350 with wireless networking option for embedded and IoT projects.",
                raspberryPi,
                Set.of(microcontroller, wifi, bluetooth));
    }

    private Tag upsertTag(String name, TagColor color, String description) {
        Tag tag = tagRepository.findByNameIgnoreCase(name).orElseGet(() -> new Tag(name, color, description));
        tag.setName(name);
        tag.setColor(color);
        tag.setDescription(description);
        return tagRepository.save(tag);
    }

    private Category upsertCategory(String name, String description, Set<Tag> tags) {
        Category category =
                categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> new Category(name, description, null));
        category.setName(name);
        category.setDescription(description);
        category.setPictureId(null);
        category.getTags().clear();
        category.getTags().addAll(tags);
        return categoryRepository.save(category);
    }

    private Part upsertPart(String name, String description, Category category, Set<Tag> tags) {
        Part part = partRepository.findByNameIgnoreCase(name).orElseGet(() -> new Part(name, description, category));
        part.setName(name);
        part.setDescription(description);
        part.setCategory(category);
        part.getTags().clear();
        part.getTags().addAll(tags);
        return partRepository.save(part);
    }
}
