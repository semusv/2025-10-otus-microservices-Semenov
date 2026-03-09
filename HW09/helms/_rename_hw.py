import os


class FileInfo:
    """Класс для хранения информации о файле"""
    def __init__(self, path, encoding='utf-8', size=0, content=None, status='unknown'):
        self.path = path
        self.encoding = encoding
        self.size = size
        self.content = content
        self.status = status  # 'found', 'not_found', 'binary', 'unreadable'


def find_HW08_in_files(root_dir, verbose=True):
    """
    Находит все файлы, содержащие HW08 (с учётом регистра)
    Проходит по всем подпапкам и файлам с детальным логированием
    """
    files_with_HW08 = []
    processed_files = []
    skipped_files = []
    stats = {
        'dirs_visited': 0,
        'files_processed': 0,
        'binary': 0,
        'unreadable': 0
    }

    # Собираем все файлы (включая скрытые)
    for dirpath, dirnames, filenames in os.walk(root_dir):
        if verbose:
            print(f"\n[📁 ПАПКА: {dirpath}]")
            print(f"   Подпапки: {dirnames[:5]}{'...' if len(dirnames) > 5 else ''}")
        
        stats['dirs_visited'] += 1
        
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            
            # Пропускаем сам скрипт
            if '_rename_hw' in filename.lower():
                if verbose:
                    print(f"   [⏭️  ПРОПУСК СКРИПТА] {file_path}")
                continue
            
            if verbose:
                print(f"   [📄 ФАЙЛ] {file_path}")
            
            stats['files_processed'] += 1
            
            # Пробуем прочитать файл
            file_info = FileInfo(
                path=file_path,
                status='binary',
                size=0
            )
            
            # Сначала пробуем бинарный режим, чтобы определить если файл бинарный
            try:
                with open(file_path, 'rb') as file:
                    header = file.read(8192)
                    
                    if b'\x00' in header:
                        if verbose:
                            print(f"      [⚠️  ВОЗМОЖНО BINARNY ФАЙЛ] - пропускаем")
                        stats['binary'] += 1
                        file_info.status = 'binary'
                        skipped_files.append(file_path)
                        continue
                    
                    file.seek(0)
                    header = file.read()
                    stats['binary'] -= 1
                    
            except Exception:
                file_info.status = 'unreadable'
                stats['unreadable'] += 1
                skipped_files.append(file_path)
                continue
            
            # Пытаемся прочитать файл с разными кодировками
            content = None
            encoding = None
            
            # Сначала пробуем UTF-8
            try:
                with open(file_path, 'r', encoding='utf-8') as file:
                    content = file.read()
                encoding = 'utf-8'
                
                if verbose:
                    print(f"      [✓ UTF-8] {len(content)} символов")
                    
            except UnicodeDecodeError:
                pass  # Пробуем другие кодировки
            
            if not content:
                encodings = ['latin-1', 'cp1251', 'cp1252', 'iso-8859-1']
                for enc in encodings:
                    try:
                        with open(file_path, 'r', encoding=enc) as file:
                            content = file.read()
                        encoding = enc
                        if verbose:
                            print(f"      [✓ {enc.upper()}] {len(content)} символов")
                        break
                    except:
                        continue
            
            if not content:
                file_info.status = 'unreadable'
                stats['unreadable'] += 1
                skipped_files.append(file_path)
                continue
            
            # Проверяем наличие всех вариантов HW (с учётом регистра: HW08, hw08, Hw08, hW08)
            if 'HW08' in content or 'hw08' in content or 'Hw08' in content or 'hW08' in content:
                # Считаем все упоминания всех вариантов
                hw8_count = content.count('HW08') + \
                           content.count('hw08') + \
                           content.count('Hw08') + \
                           content.count('hW08')
                
                file_info.encoding = encoding
                file_info.status = 'found'
                file_info.content = content
                file_info.size = len(content)
                
                if verbose:
                    print(f"      [🎯 НАЙДЕНО HW(08)] {hw8_count} упоминаний")
                
                files_with_HW08.append(file_info)
            else:
                file_info.status = 'not_found'
                file_info.encoding = encoding or 'unknown'
                file_info.size = len(content) if content else 0
                
                if verbose:
                    print(f"      [ℹ️  HW08 не найдено]")
                
                processed_files.append(file_info)
    
    # Выводим итоговую статистику
    if verbose:
        print("\n" + "=" * 60)
        print("📊 ИТОГОВАЯ СТАТИСТИКА:")
        print("=" * 60)
        print(f"  Пайдок пройдено: {stats['dirs_visited']}")
        print(f"  Файлы просмотрено: {stats['files_processed']}")
        print(f"  Файлы с HW08: {len(files_with_HW08)}")
        print(f"  Бинарные файлы: {stats['binary']}")
        print(f"  Не читаемые: {stats['unreadable']}")
        print("=" * 60)
    
    return files_with_HW08, processed_files, skipped_files, stats


def show_context(content, search_term='HW08', context_lines=1):
    """
    Показывает контекст вокруг найденного текста
    """
    lines = content.split('\n')
    result = []

    for i, line in enumerate(lines):
        if search_term in line:
            for j in range(max(0, i-context_lines), i):
                result.append(f"  {j+1}: {lines[j]}")
            result.append(f"> {i+1}: {line}")
            for j in range(i+1, min(len(lines), i+1+context_lines)):
                result.append(f"  {j+1}: {lines[j]}")
            result.append("")

    return '\n'.join(result)


def replace_with_confirmation(root_dir, verbose=True, auto_replace=False):
    """
    Показывает найденные файлы и опционально запрашивает подтверждение
    auto_replace=True - выполняется замена без подтверждения
    """
    files_with_HW08, processed_files, skipped_files, stats = find_HW08_in_files(root_dir, verbose)

    if not files_with_HW08:
        print("Файлов, содержащих 'HW08', не найдено.")
        return 0, 0

    print(f"\nНайдено файлов, содержащих 'HW08': {len(files_with_HW08)}")
    print("=" * 60)

    # Показываем найденные файлы
    total_replacements = 0
    for i, file_info in enumerate(files_with_HW08, 1):
        print(f"\n{i}. {file_info.path}")
        
        hw8_count = file_info.content.count('HW08') + \
                   file_info.content.count('hw08') + \
                   file_info.content.count('Hw08') + \
                   file_info.content.count('hW08')
        
        print(f"   Найдено упоминаний: {hw8_count}, Кодировка: {file_info.encoding}")

        if i <= 3 and not auto_replace:
            show = input(f"   Показать контекст? (y/n, Enter для пропуска): ").strip().lower()
            if show == 'y':
                print(show_context(file_info.content))

        total_replacements += hw8_count

    print("=" * 60)
    print(f"Всего будет заменено: {total_replacements} упоминаний в {len(files_with_HW08)} файлах")
    print("=" * 60)

    if not auto_replace:
        response = input("Выполнить замену? (y/n): ").strip().lower()

        if response != 'y':
            print("Операция отменена.")
            return 0, total_replacements

    # Выполняем замены с учётом регистра (HW08->HW09, hw08->hw09, Hw08->Hw09, hW08->hW09)
    processed_count = 0
    for file_info in files_with_HW08:
        try:
            new_content = file_info.content.replace('HW08', 'HW09')
            new_content = new_content.replace('hw08', 'hw09')
            new_content = new_content.replace('Hw08', 'Hw09')
            new_content = new_content.replace('hW08', 'hW09')
            
            with open(file_info.path, 'w', encoding=file_info.encoding) as file:
                file.write(new_content)

            hw8_count = file_info.content.count('HW08') + \
                       file_info.content.count('hw08') + \
                       file_info.content.count('Hw08') + \
                       file_info.content.count('hW08')
            
            print(f"\n✓ Заменено {hw8_count} упоминаний в: {file_info.path}")
            processed_count += 1
        except Exception as e:
            print(f"✗ Ошибка при обработке {file_info.path}: {e}")

    return processed_count, total_replacements


def main():
    # Получаем директорию, где находится скрипт
    script_dir = os.path.dirname(os.path.abspath(__file__))

    print("\n" + "=" * 60)
    print("📝 СКРИПТ ДЛЯ ЗАМЕНЫ 'HW08' НА 'HW09'")
    print("   (с учётом регистра: HW08->HW09, hw08->hw09, Hw08->Hw09, hW08->hW09)")
    print("=" * 60)
    print(f"Рабочая директория: {script_dir}")
    print("=" * 60)

    # Выполняем замену (автоматически, без подтверждения)
    processed_count, total_replacements = replace_with_confirmation(script_dir, auto_replace=True)

    print("\n" + "=" * 60)
    print(f"✅ Готово!")
    print(f"   Заменено файлов: {processed_count}")
    print(f"   Всего замен: {total_replacements}")
    print("=" * 60)


if __name__ == "__main__":
    main()