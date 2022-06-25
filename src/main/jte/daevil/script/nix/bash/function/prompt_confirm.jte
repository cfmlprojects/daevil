@raw
prompt_confirm() {
  while true; do
    echo "${1:-Continue?} [y/n]: "
    IFS='' read -r REPLY
    case $REPLY in
      [yY]) echo ; return 0 ;;
      [nN]) echo ; return 1 ;;
      *) printf " \033[31m %s \n\033[0m" "invalid input"
    esac
  done
}
@endraw