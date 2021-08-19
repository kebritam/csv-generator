// SPDX-License-Identifier: MIT

package ir.kebritam.generator;

import ir.kebritam.utils.Csv;
import ir.kebritam.utils.Ignore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class CsvGenerator {

    private final Object[] data;
    private final Supplier<Stream<Field>> processedFields;

    public CsvGenerator(Object[] data) {
        if (data.length < 1) {
            throw new IllegalArgumentException("data array must not be empty");
        }

        Class<?> objectsClass = data[0].getClass();

        boolean isDeviating = Arrays.stream(data).anyMatch(obj -> obj.getClass() != objectsClass);
        if (isDeviating) {
            throw new IllegalArgumentException("data array must be uniform objects");
        }

        this.data = data;
        this.processedFields = () ->
                Arrays.stream(objectsClass.getDeclaredFields())
                        .filter(field -> !field.isAnnotationPresent(Ignore.class))
                        .sorted(Comparator.comparingInt(field ->
                                field.isAnnotationPresent(Csv.class) ?
                                        field.getDeclaredAnnotation(Csv.class).columnIndex() :
                                        Integer.MAX_VALUE)
                        );
    }

    public void generate(Path fileDirectoryPath, String fileName) {
        String data = extractData();
        byte[] dataBytes = data.getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(dataBytes.length);
        buffer.put(dataBytes);
        buffer.flip();

        try (FileOutputStream outputStream = new FileOutputStream(fileDirectoryPath.toString() + "/" + fileName + ".csv");
             FileChannel channel = outputStream.getChannel()) {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractData() {
        StringBuilder builder = new StringBuilder();

        appendMetadata(builder);
        appendData(builder);

        return builder.toString();
    }

    private void appendMetadata(Appendable appendable) {
        StringJoiner joiner = newStringJoiner();

        processedFields.get()
                .map(this::getColumnName)
                .forEach(joiner::add);

        appendWithNewLine(appendable, joiner.toString());
    }

    private String getColumnName(Field field) {
        return !field.isAnnotationPresent(Csv.class) || field.getAnnotation(Csv.class).columnName().isEmpty() ?
                field.getName() :
                field.getAnnotation(Csv.class).columnName();
    }

    private void appendData(Appendable appendable) {
        for (Object obj : data) {
            StringJoiner joiner = newStringJoiner();

            processedFields.get()
                    .forEach(field -> joiner.add(getFieldValue(field, obj)));
            appendWithNewLine(appendable, joiner.toString());
        }
    }

    private String getFieldValue(Field field, Object obj) {
        String value = "";
        try {
            value = field.get(obj).toString();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    private StringJoiner newStringJoiner() {
        return new StringJoiner(", ");
    }

    private void appendWithNewLine(Appendable appendable, String line) {
        try {
            appendable.append(line).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
