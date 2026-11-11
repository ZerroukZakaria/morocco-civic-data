import { useEffect, useRef } from "react";
import { ballotLabel, formatNumber, formatTurnout } from "./format";
import { ListResults } from "./ListResults";
import type { ConstituencyDetail } from "./types";

export function ConstituencyDialog({
  detail,
  onClose,
}: {
  detail: ConstituencyDetail | null;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (detail && !dialog.open) dialog.showModal();
    if (!detail && dialog.open) dialog.close();
  }, [detail]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="constituency-title"
      onClose={onClose}
      onClick={(e) => {
        // Only the backdrop is the dialog element itself; the content sits in an inner div
        if (e.target === e.currentTarget) ref.current?.close();
      }}
    >
      {detail && (
        <div className="dialog-body">
          <div className="dialog-head">
            <h2 id="constituency-title">{detail.name}</h2>
            <button type="button" autoFocus onClick={() => ref.current?.close()}>
              Close
            </button>
          </div>

          <dl className="facts">
            <div>
              <dt>Ballot</dt>
              <dd>{ballotLabel(detail.ballot)}</dd>
            </div>
            <div>
              <dt>Region</dt>
              <dd>{detail.region}</dd>
            </div>
            {detail.wilaya && detail.wilaya !== detail.region && (
              <div>
                <dt>Wilaya</dt>
                <dd>{detail.wilaya}</dd>
              </div>
            )}
            {detail.prefectureOrProvince && (
              <div>
                <dt>Prefecture or province</dt>
                <dd>{detail.prefectureOrProvince}</dd>
              </div>
            )}
            {detail.subPrefecture && (
              <div>
                <dt>Sub-prefecture</dt>
                <dd>{detail.subPrefecture}</dd>
              </div>
            )}
            <div>
              <dt>Seats</dt>
              <dd>{detail.seats}</dd>
            </div>
            <div>
              <dt>Turnout</dt>
              <dd>{formatTurnout(detail.turnoutRate)}</dd>
            </div>
            <div>
              <dt>Votes cast for lists</dt>
              <dd>{formatNumber(detail.totalVotes)}</dd>
            </div>
          </dl>

          <ListResults lists={detail.lists} label={detail.name} />
          <p className="muted">{detail.basis}</p>
        </div>
      )}
    </dialog>
  );
}