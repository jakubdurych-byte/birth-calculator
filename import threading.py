import threading
import time
from textual.app import App, ComposeResult
from textual.widgets import Header, Footer, ListItem, ListView, Label
from textual.containers import Horizontal, Vertical
from textual.reactive import reactive
import pyperclip

class ClipItem(ListItem):
    """Custom list item to hold our clip content and type."""
    def __init__(self, content: str, clip_type: str) -> None:
        # Show a truncated single-line preview in the list
        preview = content.replace("\n", " ").strip()[:40]
        super().__init__(Label(f"[{clip_type:^7}] {preview}..."))
        self.content = content
        self.clip_type = clip_type

class DevClipApp(App):
    CSS = """
    Screen {
        background: #1a1b26;
    }
    Horizontal {
        height: 1fr;
    }
    Vertical {
        width: 1fr;
        height: 1fr;
        border: solid #3b4261;
        padding: 1;
        margin: 1;
    }
    #preview-container {
        width: 2fr;
        background: #24283c;
    }
    Label {
        color: #c0caf5;
    }
    """

    BINDINGS = [
        ("q", "quit", "Quit"),
        ("c", "copy_selected", "Copy to System"),
        ("clear", "clear_history", "Clear All")
    ]

    # Reactive list to update the UI when the daemon finds new clips
    history_items = reactive([])

    def compose(self) -> ComposeResult:
        yield Header(show_clock=True)
        with Horizontal():
            with Vertical():
                yield Label("[b]Clipboard History[/b]", id="list-title")
                yield ListView(id="clip-list")
            with Vertical(id="preview-container"):
                yield Label("[b]Preview[/b]", id="preview-title")
                yield Label("", id="preview-body")
        yield Footer()

    def on_mount(self) -> None:
        """Start the background clipboard listener when the app starts."""
        self.last_clip = ""
        self.running = True
        self.listener_thread = threading.Thread(target=self.monitor_clipboard, daemon=True)
        self.listener_thread.start()

    def monitor_clipboard(self) -> None:
        """Background thread that checks the system clipboard."""
        while self.running:
            try:
                current_clip = pyperclip.paste()
                if current_clip and current_clip != self.last_clip:
                    self.last_clip = current_clip
                    # Simple rule parser
                    clip_type = "Text"
                    if "\n" in current_clip or "def " in current_clip or "import " in current_clip:
                        clip_type = "Code"
                    elif current_clip.strip().startswith(("sudo ", "git ", "cd ", "yay ")):
                        clip_type = "Cmd"
                    
                    # Schedule UI update safely on the main thread
                    self.call_from_thread(self.add_new_clip, current_clip, clip_type)
            except Exception:
                pass
            time.sleep(0.5)

    def add_new_clip(self, content: str, clip_type: str) -> None:
        """Adds a new item to the top of the UI list."""
        clip_list = self.query_one("#clip-list", ListView)
        
        # Avoid duplicate visual entries
        for item in clip_list.children:
            if getattr(item, "content", "") == content:
                return

        new_item = ClipItem(content, clip_type)
        clip_list.insert(0, new_item)  # Push to top
        
        # Auto-select the newest item if list was empty
        if len(clip_list.children) == 1:
            clip_list.index = 0

    def on_list_view_highlighted(self, event: ListView.Highlighted) -> None:
        """Fires when navigating up/down the list to update the preview pane."""
        preview_body = self.query_one("#preview-body", Label)
        if event.item and hasattr(event.item, "content"):
            # Textual natively supports Rich syntax highlighting blocks!
            content = event.item.content
            if event.item.clip_type == "Code":
                preview_body.update(f"```python\n{content}\n```")
            elif event.item.clip_type == "Cmd":
                preview_body.update(f"```bash\n{content}\n```")
            else:
                preview_body.update(content)
        else:
            preview_body.update("")

    def action_copy_selected(self) -> None:
        """Action triggered by pressing 'c'."""
        clip_list = self.query_one("#clip-list", ListView)
        if clip_list.highlighted_child and hasattr(clip_list.highlighted_child, "content"):
            target_text = clip_list.highlighted_child.content
            self.last_clip = target_text  # Set this so the daemon doesn't loop it back in
            pyperclip.copy(target_text)
            self.notify("Copied back to system clipboard!", title="Success", severity="information")

    def action_quit(self) -> None:
        self.running = False
        self.exit()

if __name__ == "__main__":
    app = DevClipApp()
    app.run()