class SnackbarState {
  message = $state('');
  visible = $state(false);
  private timer: ReturnType<typeof setTimeout> | null = null;

  show(msg: string) {
    this.message = msg;
    this.visible = true;
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => {
      this.visible = false;
    }, 3000);
  }
}

export const snackbar = new SnackbarState();
