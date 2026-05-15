package ua.course.tvguide.repository;

import ua.course.tvguide.model.Channel;
import ua.course.tvguide.model.Program;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class TvGuideRepository {
    private final Path channelsFile;
    private final Path programsFile;
    private final List<Channel> channels = new ArrayList<>();
    private final List<Program> programs = new ArrayList<>();

    public TvGuideRepository(Path dataDirectory) {
        this.channelsFile = dataDirectory.resolve("channels.tsv");
        this.programsFile = dataDirectory.resolve("programs.tsv");
    }

    public synchronized void load() throws IOException {
        Files.createDirectories(channelsFile.getParent());
        channels.clear();
        programs.clear();

        if (Files.exists(channelsFile)) {
            for (String line : Files.readAllLines(channelsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> parts = splitLine(line);
                channels.add(new Channel(
                        Integer.parseInt(parts.get(0)),
                        parts.get(1),
                        parts.get(2)
                ));
            }
        }

        if (Files.exists(programsFile)) {
            for (String line : Files.readAllLines(programsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> parts = splitLine(line);
                programs.add(new Program(
                        Integer.parseInt(parts.get(0)),
                        Integer.parseInt(parts.get(1)),
                        parts.get(2),
                        parts.get(3),
                        parts.get(4),
                        parts.get(5),
                        parts.get(6),
                        parts.get(7),
                        Integer.parseInt(parts.get(8))
                ));
            }
        }
    }

    public synchronized List<Channel> listChannels() {
        return channels.stream()
                .map(this::copyChannel)
                .sorted(Comparator.comparing(Channel::getName))
                .toList();
    }

    public synchronized List<Program> listPrograms(Optional<Integer> channelId, String sort) {
        Comparator<Program> comparator = Comparator
                .comparing(Program::getDate)
                .thenComparing(Program::getStartTime)
                .thenComparing(Program::getTitle);

        if ("channel".equalsIgnoreCase(sort)) {
            comparator = Comparator
                    .comparing((Program program) -> channelName(program.getChannelId()))
                    .thenComparing(Program::getDate)
                    .thenComparing(Program::getStartTime);
        }

        return programs.stream()
                .filter(program -> channelId.map(id -> program.getChannelId() == id).orElse(true))
                .map(this::copyProgram)
                .sorted(comparator)
                .toList();
    }

    public synchronized Optional<Channel> findChannel(int id) {
        return channels.stream()
                .filter(channel -> channel.getId() == id)
                .findFirst()
                .map(this::copyChannel);
    }

    public synchronized String channelName(int id) {
        return channels.stream()
                .filter(channel -> channel.getId() == id)
                .map(Channel::getName)
                .findFirst()
                .orElse("Невідомий канал");
    }

    public synchronized Channel createChannel(Channel channel) throws IOException {
        channel.setId(nextChannelId());
        validateChannel(channel);
        channels.add(copyChannel(channel));
        saveChannels();
        return copyChannel(channel);
    }

    public synchronized Channel updateChannel(int id, Channel updated) throws IOException {
        validateChannel(updated);
        Channel channel = channels.stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Канал не знайдено"));

        channel.setName(updated.getName());
        channel.setCategory(updated.getCategory());
        saveChannels();
        return copyChannel(channel);
    }

    public synchronized void deleteChannel(int id) throws IOException {
        boolean removed = channels.removeIf(channel -> channel.getId() == id);
        if (!removed) {
            throw new NoSuchElementException("Канал не знайдено");
        }
        programs.removeIf(program -> program.getChannelId() == id);
        saveChannels();
        savePrograms();
    }

    public synchronized Program createProgram(Program program) throws IOException {
        program.setId(nextProgramId());
        validateProgram(program);
        programs.add(copyProgram(program));
        savePrograms();
        return copyProgram(program);
    }

    public synchronized Program updateProgram(int id, Program updated) throws IOException {
        validateProgram(updated);
        Program program = programs.stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Передачу не знайдено"));

        program.setChannelId(updated.getChannelId());
        program.setTitle(updated.getTitle());
        program.setDescription(updated.getDescription());
        program.setDate(updated.getDate());
        program.setStartTime(updated.getStartTime());
        program.setEndTime(updated.getEndTime());
        program.setGenre(updated.getGenre());
        program.setAgeRating(updated.getAgeRating());
        savePrograms();
        return copyProgram(program);
    }

    public synchronized void deleteProgram(int id) throws IOException {
        boolean removed = programs.removeIf(program -> program.getId() == id);
        if (!removed) {
            throw new NoSuchElementException("Передачу не знайдено");
        }
        savePrograms();
    }

    private int nextChannelId() {
        return channels.stream().mapToInt(Channel::getId).max().orElse(0) + 1;
    }

    private int nextProgramId() {
        return programs.stream().mapToInt(Program::getId).max().orElse(0) + 1;
    }

    private void validateChannel(Channel channel) {
        if (isBlank(channel.getName())) {
            throw new IllegalArgumentException("Назва каналу є обов'язковою");
        }
        if (isBlank(channel.getCategory())) {
            throw new IllegalArgumentException("Категорія каналу є обов'язковою");
        }
    }

    private void validateProgram(Program program) {
        if (findChannel(program.getChannelId()).isEmpty()) {
            throw new IllegalArgumentException("Оберіть існуючий канал");
        }
        if (isBlank(program.getTitle())) {
            throw new IllegalArgumentException("Назва передачі є обов'язковою");
        }
        if (isBlank(program.getDescription())) {
            throw new IllegalArgumentException("Опис передачі є обов'язковим");
        }
        if (isBlank(program.getGenre())) {
            throw new IllegalArgumentException("Жанр передачі є обов'язковим");
        }

        LocalTime start;
        LocalTime end;
        try {
            LocalDate.parse(program.getDate());
            start = LocalTime.parse(program.getStartTime());
            end = LocalTime.parse(program.getEndTime());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Дата або час мають некоректний формат");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Час завершення має бути пізніше часу початку");
        }
        if (program.getAgeRating() < 0 || program.getAgeRating() > 18) {
            throw new IllegalArgumentException("Віковий рейтинг має бути від 0 до 18");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void saveChannels() throws IOException {
        List<String> lines = channels.stream()
                .sorted(Comparator.comparingInt(Channel::getId))
                .map(channel -> joinLine(
                        String.valueOf(channel.getId()),
                        channel.getName(),
                        channel.getCategory()
                ))
                .toList();
        Files.write(channelsFile, lines, StandardCharsets.UTF_8);
    }

    private void savePrograms() throws IOException {
        List<String> lines = programs.stream()
                .sorted(Comparator.comparingInt(Program::getId))
                .map(program -> joinLine(
                        String.valueOf(program.getId()),
                        String.valueOf(program.getChannelId()),
                        program.getTitle(),
                        program.getDescription(),
                        program.getDate(),
                        program.getStartTime(),
                        program.getEndTime(),
                        program.getGenre(),
                        String.valueOf(program.getAgeRating())
                ))
                .toList();
        Files.write(programsFile, lines, StandardCharsets.UTF_8);
    }

    private String joinLine(String... values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            escaped.add(escape(value));
        }
        return String.join("\t", escaped);
    }

    private List<String> splitLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (char character : line.toCharArray()) {
            if (escaping) {
                current.append(switch (character) {
                    case 't' -> '\t';
                    case 'n' -> '\n';
                    case '\\' -> '\\';
                    default -> character;
                });
                escaping = false;
                continue;
            }

            if (character == '\\') {
                escaping = true;
            } else if (character == '\t') {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        result.add(current.toString());
        return result;
    }

    private String escape(String value) {
        return Optional.ofNullable(value).orElse("")
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    private Channel copyChannel(Channel channel) {
        return new Channel(channel.getId(), channel.getName(), channel.getCategory());
    }

    private Program copyProgram(Program program) {
        return new Program(
                program.getId(),
                program.getChannelId(),
                program.getTitle(),
                program.getDescription(),
                program.getDate(),
                program.getStartTime(),
                program.getEndTime(),
                program.getGenre(),
                program.getAgeRating()
        );
    }
}
