import os
import sys

def find_hw07_in_files(root_dir):
    """
    Находит все файлы, содержащие hw07
    """
    files_with_hw07 = []

    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)

            # Пропускаем сам скрипт
            if file_path.endswith('.py') and '_rename_hw' in filename.lower():
                continue

            # Пытаемся прочитать файл
            try:
                # Пробуем UTF-8
                with open(file_path, 'r', encoding='utf-8') as file:
                    content = file.read()

                if 'hw07' in content:
                    count = content.count('hw07')
                    files_with_hw07.append((file_path, count, 'utf-8', content))

            except UnicodeDecodeError:
                # Пробуем другие кодировки
                encodings = ['latin-1', 'cp1251', 'cp1252', 'iso-8859-1']
                for encoding in encodings:
                    try:
                        with open(file_path, 'r', encoding=encoding) as file:
                            content = file.read()

                        if 'hw07' in content:
                            count = content.count('hw07')
                            files_with_hw07.append((file_path, count, encoding, content))
                            break
                    except:
                        continue
            except:
                # Пропускаем файлы, которые не удалось прочитать
                pass

    return files_with_hw07

def show_context(content, search_term='hw07', context_lines=1):
    """
    Показывает контекст вокруг найденного текста
    """
    lines = content.split('\n')
    result = []

    for i, line in enumerate(lines):
        if search_term in line:
            # Добавляем предыдущие строки контекста
            for j in range(max(0, i-context_lines), i):
                result.append(f"  {j+1}: {lines[j]}")

            # Добавляем строку с найденным текстом
            result.append(f"> {i+1}: {line}")

            # Добавляем следующие строки контекста
            for j in range(i+1, min(len(lines), i+1+context_lines)):
                result.append(f"  {j+1}: {lines[j]}")

            result.append("")  # Пустая строка для разделения

    return '\n'.join(result)

def replace_with_confirmation(root_dir):
    """
    Показывает найденные файлы и запрашивает подтверждение
    """
    files_with_hw07 = find_hw07_in_files(root_dir)

    if not files_with_hw07:
        print("Файлов, содержащих 'hw07', не найдено.")
        return 0

    print(f"Найдено файлов, содержащих 'hw07': {len(files_with_hw07)}")
    print("=" * 60)

    # Показываем найденные файлы
    total_replacements = 0
    for i, (file_path, count, encoding, content) in enumerate(files_with_hw07, 1):
        print(f"{i}. {file_path}")
        print(f"   Найдено упоминаний: {count}, Кодировка: {encoding}")

        # Показываем контекст для первых 3 файлов или по запросу
        if i <= 3 or input(f"   Показать контекст для этого файла? (y/n, Enter для пропуска): ").lower() == 'y':
            print(show_context(content))

        total_replacements += count

    print("=" * 60)
    print(f"Всего будет заменено: {total_replacements} упоминаний в {len(files_with_hw07)} файлах")
    print("=" * 60)

    # Запрашиваем подтверждение
    response = input("Выполнить замену? (y/n): ").strip().lower()

    if response != 'y':
        print("Операция отменена.")
        return 0

    # Выполняем замену
    processed_count = 0
    for file_path, count, encoding, content in files_with_hw07:
        try:
            new_content = content.replace('hw07', 'hw08')
            with open(file_path, 'w', encoding=encoding) as file:
                file.write(new_content)

            print(f"✓ Заменено {count} упоминаний в: {file_path}")
            processed_count += 1
        except Exception as e:
            print(f"✗ Ошибка при обработке {file_path}: {e}")

    return processed_count, total_replacements

def main():
    # Получаем директорию, где находится скрипт
    script_dir = os.path.dirname(os.path.abspath(__file__))

    print("Скрипт для замены 'hw07' на 'hw08' в содержимом файлов")
    print(f"Рабочая директория: {script_dir}")
    print("=" * 60)

    # Выполняем замену с подтверждением
    processed_files, total_replacements = replace_with_confirmation(script_dir)

    print("=" * 60)
    print(f"Готово! Обработано файлов: {processed_files}")
    print(f"Всего заменено упоминаний: {total_replacements}")

    if sys.platform == 'win32':
        input("\nНажмите Enter для выхода...")

if __name__ == "__main__":
    main()