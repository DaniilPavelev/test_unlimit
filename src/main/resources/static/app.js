(() => {
  const descriptionEl = document.getElementById("description");
  const analyzeBtn = document.getElementById("analyze");
  const loadingEl = document.getElementById("loading");
  const errorEl = document.getElementById("error");
  const resultEl = document.getElementById("result");
  const historyEl = document.getElementById("history");

  function showError(message) {
    errorEl.hidden = false;
    errorEl.textContent = message;
  }

  function clearError() {
    errorEl.hidden = true;
    errorEl.textContent = "";
  }

  function setLoading(isLoading) {
    loadingEl.hidden = !isLoading;
    analyzeBtn.disabled = isLoading;
  }

  function renderResult(analysis) {
    resultEl.classList.remove("empty");
    resultEl.textContent = "";

    const category = document.createElement("div");
    category.textContent = "Category: " + analysis.category;
    resultEl.appendChild(category);

    const severity = document.createElement("div");
    severity.textContent = "Severity: " + analysis.severity;
    resultEl.appendChild(severity);

    const summary = document.createElement("p");
    summary.textContent = analysis.summary;
    resultEl.appendChild(summary);

    (analysis.hypotheses || []).forEach((hypothesis, index) => {
      const block = document.createElement("div");
      block.className = "hypothesis";

      const title = document.createElement("strong");
      title.textContent = (index + 1) + ". " + hypothesis.title;
      block.appendChild(title);

      const reasoning = document.createElement("p");
      reasoning.textContent = hypothesis.reasoning;
      block.appendChild(reasoning);

      const stepsTitle = document.createElement("div");
      stepsTitle.textContent = "Next steps:";
      block.appendChild(stepsTitle);

      const list = document.createElement("ol");
      (hypothesis.nextSteps || []).forEach((step) => {
        const li = document.createElement("li");
        li.textContent = step;
        list.appendChild(li);
      });
      block.appendChild(list);
      resultEl.appendChild(block);
    });

    const meta = document.createElement("div");
    meta.className = "meta";
    meta.textContent = "History: " + (analysis.metadata.selectedHistoricalAnalysisIds || []).join(", ")
      + " | Attempts: " + analysis.metadata.llmAttempts;
    resultEl.appendChild(meta);
  }

  async function loadHistory() {
    const response = await fetch("/api/v1/incident-analyses");
    if (!response.ok) {
      return;
    }
    const data = await response.json();
    historyEl.textContent = "";
    if (!data.items || data.items.length === 0) {
      historyEl.classList.add("empty");
      historyEl.textContent = "No history yet.";
      return;
    }
    historyEl.classList.remove("empty");
    data.items.forEach((item) => {
      const row = document.createElement("div");
      row.className = "history-item";
      row.textContent = item.createdAt + " | " + item.severity + " | " + item.category + " — " + item.summary;
      historyEl.appendChild(row);
    });
  }

  analyzeBtn.addEventListener("click", async () => {
    clearError();
    setLoading(true);
    try {
      const response = await fetch("/api/v1/incident-analyses", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ description: descriptionEl.value })
      });
      const payload = await response.json();
      if (!response.ok) {
        const detail = payload.detail || "Request failed";
        const fieldErrors = Array.isArray(payload.errors)
          ? payload.errors.map((e) => (e.field ? e.field + ": " + e.message : e)).join("\n")
          : "";
        throw new Error(detail + (fieldErrors ? "\n" + fieldErrors : ""));
      }
      renderResult(payload);
      await loadHistory();
    } catch (error) {
      showError(error.message || String(error));
    } finally {
      setLoading(false);
    }
  });

  loadHistory().catch(() => {});
})();
