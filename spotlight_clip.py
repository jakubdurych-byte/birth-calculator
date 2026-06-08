import sys
import threading
import time
from PyQt6.QtCore import Qt, pyqtSignal, QObject
from PyQt6.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout,
    QLineEdit, QListWidget, QListWidgetItem, QGraphicsDropShadowEffect, QLabel
)
from PyQt6.QtGui import QColor, QFont, QKeyEvent
import pyperclip


class ClipboardWorker(QObject):
    """Background thread to monitor the system clipboard cleanly."""
    new_item_signal = pyqtSignal(str, str)

    def __init__(self):
        super().__init__()
        self.last_clip = ""
        self.running = True

    def start_monitoring(self):
        while self.running:
            try:
                current_clip = pyperclip.paste()
                if current_clip and current_clip != self.last_clip:
                    self.last_clip = current_clip

                    # Basic classification rules
                    clip_type = "TEXT"
                    if "\n" in current_clip or "def " in current_clip or "{" in current_clip:
                        clip_type = "CODE"
                    elif current_clip.strip().startswith(("sudo ", "git ", "cd ", "yay ", "pip ")):
                        clip_type = "CMD"

                    self.new_item_signal.emit(current_clip, clip_type)
            except Exception:
                pass
            time.sleep(0.3)


class SpotlightClip(QWidget):
    def __init__(self):
        super().__init__()
        self.all_clips = []  # Stores tuples: (raw_content, display_text)
        self.init_ui()
        self.start_backend_worker()

    def init_ui(self):
        # 1. Stripping Window Borders & Framing entirely
        self.setWindowFlags(
            Qt.WindowType.FramelessWindowHint |  # No system titlebar or thick borders
            Qt.WindowType.WindowStaysOnTopHint |  # Keep floating above normal application layers
            Qt.WindowType.Tool  # Skip taskbars and workspaces lists
        )
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground, True)

        # Explicit window class identifier for window manager targeting rules
        self.setClassName("spotlight-clipboard")

        # Dimensions matching the mock-up layout
        self.setFixedSize(680, 440)
        self.center_on_screen()

        # 2. Main Layout Container
        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(15, 15, 15, 15)

        # Central highly transparent wrapper card matching the image
        self.central_widget = QWidget(self)
        self.central_widget.setObjectName("CentralWidget")

        # Premium dark glass stylesheet
        self.central_widget.setStyleSheet("""
            QWidget#CentralWidget {
                background-color: rgba(24, 24, 37, 0.65); /* High transparency for compositor blur */
                border: 1px solid rgba(255, 255, 255, 0.08);
                border-radius: 16px;
            }
            QLineEdit {
                background-color: rgba(0, 0, 0, 0.25);
                border: 1px solid rgba(255, 255, 255, 0.08);
                border-radius: 10px;
                padding: 12px 14px;
                color: #cdd6f4;
                selection-background-color: #f5c2e7;
            }
            QLineEdit:focus {
                border: 1px solid rgba(245, 194, 231, 0.5);
            }
            QListWidget {
                background: transparent;
                border: none;
                color: #a6adc8;
            }
            QListWidget::item {
                background-color: rgba(255, 255, 255, 0.02);
                padding: 12px;
                margin-top: 5px;
                border-radius: 8px;
            }
            QListWidget::item:hover {
                background-color: rgba(255, 255, 255, 0.06);
                color: #cdd6f4;
            }
            QListWidget::item:selected {
                background-color: rgba(245, 194, 231, 0.15);
                color: #f5c2e7;
                border: 1px solid rgba(245, 194, 231, 0.3);
            }
            QLabel {
                color: #a6adc8;
            }
        """)

        widget_layout = QVBoxLayout(self.central_widget)
        widget_layout.setContentsMargins(18, 18, 18, 14)
        widget_layout.setSpacing(12)

        # 3. Search Bar Input Component
        self.search_bar = QLineEdit(self)
        self.search_bar.setPlaceholderText("Search clipboard history...")
        self.search_bar.setFont(QFont("Fira Code", 11) if QFont("Fira Code").exactMatch() else QFont("Sans Serif", 12))
        self.search_bar.textChanged.connect(self.filter_history)
        widget_layout.addWidget(self.search_bar)

        # 4. History List Element
        self.history_list = QListWidget(self)
        self.history_list.setFont(QFont("Sans Serif", 11))
        self.history_list.itemActivated.connect(self.select_item)
        widget_layout.addWidget(self.history_list)

        # 5. Bottom Status Action Bar
        action_layout = QHBoxLayout()
        action_layout.setContentsMargins(5, 4, 5, 2)

        shortcut_font = QFont("Sans Serif", 9)
        self.copy_hint = QLabel("📋 Copy [Enter]", self)
        self.copy_hint.setFont(shortcut_font)
        self.copy_hint.setStyleSheet("color: rgba(205, 214, 244, 0.5);")

        self.dismiss_hint = QLabel("❌ Close [Esc]", self)
        self.dismiss_hint.setFont(shortcut_font)
        self.dismiss_hint.setStyleSheet("color: rgba(205, 214, 244, 0.5);")

        action_layout.addWidget(self.copy_hint)
        action_layout.addStretch()
        action_layout.addWidget(self.dismiss_hint)
        widget_layout.addLayout(action_layout)

        # Subtle outward drop-shadow for separation
        shadow = QGraphicsDropShadowEffect(self)
        shadow.setBlurRadius(30)
        shadow.setColor(QColor(0, 0, 0, 160))
        shadow.setOffset(0, 8)
        self.central_widget.setGraphicsEffect(shadow)

        main_layout.addWidget(self.central_widget)
        self.search_bar.setFocus()

    def setClassName(self, name: str):
        """Helper to assign a predictable Wayland app_id/class name."""
        self.setObjectName(name)
        self.setWindowRole(name)

    def center_on_screen(self):
        screen = QApplication.primaryScreen().geometry()
        x = (screen.width() - self.width()) // 2
        y = int(screen.height() * 0.22)  # Balanced 1/5 layout height drop down
        self.move(x, y)

    def start_backend_worker(self):
        self.worker = ClipboardWorker()
        self.worker.new_item_signal.connect(self.handle_new_clip)
        self.thread = threading.Thread(target=self.worker.start_monitoring, daemon=True)
        self.thread.start()

    def handle_new_clip(self, content, clip_type):
        display_line = content.replace("\n", " ").strip()
        if len(display_line) > 65:
            display_line = display_line[:65] + "..."

        display_text = f" [{clip_type}]   {display_line}"
        self.all_clips.insert(0, (content, display_text))
        self.filter_history()

    def filter_history(self):
        query = self.search_bar.text().lower()
        self.history_list.clear()

        for content, display_text in self.all_clips:
            if query in content.lower():
                item = QListWidgetItem(display_text)
                item.setData(Qt.ItemDataRole.UserRole, content)
                self.history_list.addItem(item)

        if self.history_list.count() > 0:
            self.history_list.setCurrentRow(0)

    def select_item(self, item):
        if item:
            raw_text = item.data(Qt.ItemDataRole.UserRole)
            self.worker.last_clip = raw_text
            pyperclip.copy(raw_text)
            self.close()

    def keyPressEvent(self, event: QKeyEvent):
        if event.key() == Qt.Key.Key_Escape:
            self.close()
        elif event.key() == Qt.Key.Key_Down:
            self.history_list.setFocus()
        elif event.key() == Qt.Key.Key_Return or event.key() == Qt.Key.Key_Enter:
            self.select_item(self.history_list.currentItem())
        else:
            super().keyPressEvent(event)


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = SpotlightClip()
    window.show()
    sys.exit(app.exec())